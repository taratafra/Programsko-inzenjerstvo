import { useEffect, useRef, useState } from 'react';
import { useAuth0 } from "@auth0/auth0-react";
import { useToast } from './ToastNotification';

export default function NotificationService() {
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const { addToast } = useToast();

    const pollingIntervalRef = useRef(null);
    const isFetchingRef = useRef(false);
    const displayedNotificationsRef = useRef(new Set());

    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;
    const POLL_INTERVAL = 5000;

    // ✅ Reactive local token state
    const [localToken, setLocalToken] = useState(localStorage.getItem("token"));

    // ✅ Watch for token being added after login
    useEffect(() => {
        const interval = setInterval(() => {
            const token = localStorage.getItem("token");
            setLocalToken(prev => (prev !== token ? token : prev));
        }, 500);

        return () => clearInterval(interval);
    }, []);

    const getToken = async () => {
        if (isAuthenticated) {
            return await getAccessTokenSilently({
                authorizationParams: {
                    audience: `${AUDIENCE}`,
                    scope: "openid profile email",
                },
            });
        }

        if (!localToken) {
            throw new Error("No authentication token found");
        }

        return localToken;
    };

    const fetchNotifications = async () => {
        if (isFetchingRef.current) {
            console.log('⏭️ Skipping fetch - already in progress');
            return;
        }

        isFetchingRef.current = true;

        try {
            const token = await getToken();

            console.log('📡 Fetching notifications at:', new Date().toLocaleTimeString());

            const response = await fetch(`${BACKEND_URL}/api/notifications/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            console.log('📥 Response status:', response.status);

            if (!response.ok) return;

            const notifications = await response.json();

            if (!notifications?.length) {
                console.log('📭 No notifications from backend');
                return;
            }

            const newNotifications = notifications.filter(
                notif => !displayedNotificationsRef.current.has(notif.id)
            );

            if (!newNotifications.length) return;

            console.log(`🔔 Displaying ${newNotifications.length} new notification(s)`);

            newNotifications.forEach((notif) => {
                let toastType = "info";
                const text = `${notif.title ?? ""} ${notif.message}`.toLowerCase();

                if (text.includes("reminder") || text.includes("starts in")) toastType = "reminder";
                else if (text.includes("success") || text.includes("completed")) toastType = "success";
                else if (text.includes("error") || text.includes("failed")) toastType = "error";
                else if (text.includes("warning") || text.includes("alert")) toastType = "warning";

                const message = notif.title
                    ? `${notif.title}: ${notif.message}`
                    : notif.message;

                console.log('🎯 Showing toast:', { message, toastType });

                addToast(message, toastType, 8000);
                displayedNotificationsRef.current.add(notif.id);
            });

            const notificationIds = newNotifications.map(n => n.id);

            await fetch(`${BACKEND_URL}/api/notifications/mark-read`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(notificationIds),
            });

        } catch (err) {
            console.error("❌ NotificationService error:", err);
        } finally {
            isFetchingRef.current = false;
        }
    };

    // ✅ Main polling effect now reacts to token availability
    useEffect(() => {
        if (!isAuthenticated && !localToken) return;

        console.log('🎯 NotificationService started');
        console.log('🕐 Current time:', new Date().toLocaleTimeString());

        fetchNotifications();

        pollingIntervalRef.current = setInterval(() => {
            console.log('⏰ Polling tick at:', new Date().toLocaleTimeString());
            fetchNotifications();
        }, POLL_INTERVAL);

        return () => {
            console.log('🛑 Stopping notification polling');
            clearInterval(pollingIntervalRef.current);
        };
    }, [isAuthenticated, localToken]);

    return null;
}
