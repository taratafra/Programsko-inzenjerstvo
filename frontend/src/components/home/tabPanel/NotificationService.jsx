import { useEffect, useRef } from 'react';
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

    const getToken = async () => {
        if (isAuthenticated) {
            return await getAccessTokenSilently({
                authorizationParams: {
                    audience: `${AUDIENCE}`,
                    scope: "openid profile email",
                },
            });
        }

        const localToken = localStorage.getItem("token");
        if (!localToken) {
            throw new Error("No authentication token found");
        }
        return localToken;
    };

    const fetchNotifications = async () => {
        if (isFetchingRef.current) {
            console.log('â­ï¸ Skipping fetch - already in progress');
            return;
        }

        isFetchingRef.current = true;

        try {
            const token = await getToken();
            
            console.log('ðŸ“¡ Fetching notifications at:', new Date().toLocaleTimeString());

            const response = await fetch(`${BACKEND_URL}/api/notifications/me`, {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });

            console.log('ðŸ“¥ Response status:', response.status);

            if (!response.ok) {
                console.error("Failed to fetch notifications, status:", response.status);
                return;
            }

            const notifications = await response.json();
            
            // ðŸ” DEBUG: Log all notifications received
            console.log('ðŸ“¬ Raw notifications from backend:', notifications);
            console.log('ðŸ“¬ Total notifications received:', notifications?.length || 0);

            if (!notifications || notifications.length === 0) {
                console.log('ðŸ“­ No notifications from backend');
                return;
            }

            // ðŸ” DEBUG: Log each notification details
            notifications.forEach(notif => {
                console.log('ðŸ”” Notification details:', {
                    id: notif.id,
                    title: notif.title,
                    message: notif.message,
                    fireAt: notif.fireAt,
                    isRead: notif.isRead,
                    createdAt: notif.createdAt,
                    alreadyDisplayed: displayedNotificationsRef.current.has(notif.id)
                });
            });

            const newNotifications = notifications.filter(
                notif => !displayedNotificationsRef.current.has(notif.id)
            );

            if (newNotifications.length === 0) {
                console.log('ðŸ”• No new notifications to display (all already shown)');
                return;
            }

            console.log(`ðŸ”” Displaying ${newNotifications.length} new notification(s)`);

            newNotifications.forEach((notif) => {
                let toastType = "info";
                const text = `${notif.title ?? ""} ${notif.message}`.toLowerCase();

                if (text.includes("reminder") || text.includes("starts in")) {
                    toastType = "reminder";
                } else if (text.includes("success") || text.includes("completed")) {
                    toastType = "success";
                } else if (text.includes("error") || text.includes("failed")) {
                    toastType = "error";
                } else if (text.includes("warning") || text.includes("alert")) {
                    toastType = "warning";
                }

                const message = notif.title
                    ? `${notif.title}: ${notif.message}`
                    : notif.message;

                console.log('ðŸŽ¯ Showing toast:', { message, toastType });
                addToast(message, toastType, 8000);
                
                displayedNotificationsRef.current.add(notif.id);
            });

            const notificationIds = newNotifications.map((n) => n.id);
            
            console.log('âœ… Marking as read:', notificationIds);
            
            await fetch(`${BACKEND_URL}/api/notifications/mark-read`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(notificationIds),
            });

        } catch (err) {
            console.error("âŒ NotificationService error:", err);
        } finally {
            isFetchingRef.current = false;
        }
    };

    useEffect(() => {
        const localToken = localStorage.getItem("token");
        if (!isAuthenticated && !localToken) return;

        console.log('ðŸŽ¯ NotificationService mounted - starting polling every 5 seconds');
        console.log('ðŸ• Current time:', new Date().toLocaleTimeString());

        // Initial fetch
        fetchNotifications();

        // Set up polling
        pollingIntervalRef.current = setInterval(() => {
            console.log('â° Polling tick at:', new Date().toLocaleTimeString());
            fetchNotifications();
        }, POLL_INTERVAL);

        return () => {
            console.log('ðŸ›‘ NotificationService unmounting - stopping polling');
            if (pollingIntervalRef.current) {
                clearInterval(pollingIntervalRef.current);
            }
        };
    }, [isAuthenticated]);

    return null;
}