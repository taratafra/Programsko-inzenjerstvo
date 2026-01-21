import { useEffect, useMemo, useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./AdminDashboard.module.css";

export default function AdminDashboard({ setActiveTab }) {
    const [activeSection, setActiveSection] = useState("overview");

    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    const [users, setUsers] = useState([]);
    const [trainers, setTrainers] = useState([]);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

    const normalizeRole = (role) => (role || "").toString().trim().toUpperCase();

    // Robustly read banned flag regardless of backend naming
    const getIsBanned = (obj) => {
        if (!obj) return false;

        // most common
        if (typeof obj.isBanned === "boolean") return obj.isBanned;
        if (typeof obj.banned === "boolean") return obj.banned;

        // other possible spellings
        if (typeof obj.is_banned === "boolean") return obj.is_banned;
        if (typeof obj.ban === "boolean") return obj.ban;

        // nested shapes (just in case)
        if (obj.user && typeof obj.user.isBanned === "boolean") return obj.user.isBanned;
        if (obj.user && typeof obj.user.banned === "boolean") return obj.user.banned;

        return false;
    };

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
        if (!localToken) throw new Error("No authentication token found");
        return localToken;
    };

    const fetchUsers = async () => {
        setLoading(true);
        setError("");
        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/users`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to fetch users (${res.status})`);
            }

            const data = await res.json();
            setUsers(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("fetchUsers error:", e);
            setError(e?.message || "Failed to load users");
            setUsers([]);
        } finally {
            setLoading(false);
        }
    };

    const fetchTrainers = async () => {
        setLoading(true);
        setError("");
        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/trainers`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to fetch trainers (${res.status})`);
            }

            const data = await res.json();
            setTrainers(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("fetchTrainers error:", e);
            setError(e?.message || "Failed to load trainers");
            setTrainers([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (activeSection === "users") fetchUsers();
        if (activeSection === "trainers") fetchTrainers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeSection]);

    const matchesSearch = (item, q) => {
        const haystack = [item?.id, item?.email, item?.name, item?.surname, item?.role]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();
        return haystack.includes(q);
    };

    // User tab: exclude ADMIN and TRAINER (trainers live in Trainer tab)
    const userRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return users
            .filter((u) => normalizeRole(u?.role) !== "ADMIN")
            .filter((u) => normalizeRole(u?.role) !== "TRAINER")
            .filter((u) => (q ? matchesSearch(u, q) : true));
    }, [users, search]);

    // Trainer tab: fetched from /api/trainers
    const trainerRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return trainers.filter((t) => (q ? matchesSearch(t, q) : true));
    }, [trainers, search]);

    const refreshActive = async () => {
        if (activeSection === "users") return fetchUsers();
        if (activeSection === "trainers") return fetchTrainers();
    };

    /**
     * listType:
     *  - "users": uses /api/admins/users/{id}/ban|unban
     *  - "trainers": uses /api/admins/trainers/{id}/ban|unban
     */
    const setBanStatus = async (id, banned, listType) => {
        const endpoint = banned ? "ban" : "unban";
        const basePath = listType === "trainers" ? "trainers" : "users";

        try {
            const token = await getToken();
            const res = await fetch(
                `${BACKEND_URL}/api/admins/${basePath}/${id}/${endpoint}`,
                {
                    method: "PATCH",
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to ${endpoint} (${res.status})`);
            }

            // optimistic update: set multiple keys so UI stays consistent
            if (listType === "users") {
                setUsers((prev) =>
                    prev.map((u) =>
                        u.id === id
                            ? {
                                  ...u,
                                  isBanned: banned,
                                  banned,
                                  is_banned: banned, // in case backend uses this
                              }
                            : u
                    )
                );
            } else {
                setTrainers((prev) =>
                    prev.map((t) =>
                        t.id === id
                            ? {
                                  ...t,
                                  isBanned: banned,
                                  banned,
                                  is_banned: banned,
                              }
                            : t
                    )
                );
            }
        } catch (e) {
            console.error(`setBanStatus ${endpoint} error:`, e);
            alert(e?.message || `Failed to ${endpoint}`);
        }
    };

    const renderTable = (rows, listType) => (
        <div className={styles.tableWrap}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th className={styles.actionsCol}>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {!loading &&
                        rows.map((u) => {
                            const fullName =
                                `${u?.name || ""} ${u?.surname || ""}`.trim() || "-";
                            const isBanned = getIsBanned(u);

                            return (
                                <tr key={u.id}>
                                    <td>{u.id}</td>
                                    <td>{fullName}</td>
                                    <td>{u.email || "-"}</td>
                                    <td>{u.role || "-"}</td>
                                    <td>
                                        <span
                                            className={`${styles.badge} ${
                                                isBanned
                                                    ? styles.badgeBanned
                                                    : styles.badgeActive
                                            }`}
                                        >
                                            {isBanned ? "Banned" : "Active"}
                                        </span>
                                    </td>
                                    <td className={styles.actionsCol}>
                                        {isBanned ? (
                                            <button
                                                className={`${styles.actionBtn} ${styles.unbanBtn}`}
                                                onClick={() => setBanStatus(u.id, false, listType)}
                                            >
                                                Unban
                                            </button>
                                        ) : (
                                            <button
                                                className={`${styles.actionBtn} ${styles.banBtn}`}
                                                onClick={() => setBanStatus(u.id, true, listType)}
                                            >
                                                Ban
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}

                    {!loading && rows.length === 0 && (
                        <tr>
                            <td colSpan={6} className={styles.emptyRow}>
                                No results found.
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    );

    return (
        <div className={styles.adminContainer}>
            <div className={styles.header}>
                <h1>Admin Dashboard</h1>
                <p>Manage users, trainers, and system settings</p>
            </div>

            <div className={styles.navigationTabs}>
                <button
                    className={activeSection === "overview" ? styles.active : ""}
                    onClick={() => setActiveSection("overview")}
                >
                    Overview
                </button>
                <button
                    className={activeSection === "users" ? styles.active : ""}
                    onClick={() => setActiveSection("users")}
                >
                    User Management
                </button>
                <button
                    className={activeSection === "trainers" ? styles.active : ""}
                    onClick={() => setActiveSection("trainers")}
                >
                    Trainer Management
                </button>
                <button
                    className={activeSection === "content" ? styles.active : ""}
                    onClick={() => setActiveSection("content")}
                >
                    Content
                </button>
            </div>

            <div className={styles.content}>
                {activeSection === "overview" && (
                    <div className={styles.section}>
                        <h2>System Overview</h2>
                        <div className={styles.statsGrid}>
                            <div className={styles.statCard}>
                                <h3>Total Users</h3>
                                <p className={styles.statNumber}>-</p>
                            </div>
                            <div className={styles.statCard}>
                                <h3>Total Trainers</h3>
                                <p className={styles.statNumber}>-</p>
                            </div>
                            <div className={styles.statCard}>
                                <h3>Active Sessions</h3>
                                <p className={styles.statNumber}>-</p>
                            </div>
                            <div className={styles.statCard}>
                                <h3>Total Content</h3>
                                <p className={styles.statNumber}>-</p>
                            </div>
                        </div>
                    </div>
                )}

                {(activeSection === "users" || activeSection === "trainers") && (
                    <div className={styles.section}>
                        <div className={styles.sectionHeaderRow}>
                            <div>
                                <h2>
                                    {activeSection === "users"
                                        ? "User Management"
                                        : "Trainer Management"}
                                </h2>
                                <p className={styles.subtle}>
                                    {activeSection === "users"
                                        ? "Users (admins excluded; trainers shown in Trainer tab)"
                                        : "Trainer accounts (from /api/trainers)"}
                                </p>
                            </div>

                            <button
                                className={styles.refreshBtn}
                                onClick={refreshActive}
                                disabled={loading}
                            >
                                {loading ? "Loading..." : "Refresh"}
                            </button>
                        </div>

                        <div className={styles.toolbar}>
                            <input
                                className={styles.searchInput}
                                placeholder="Search by name, email, role, id…"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>

                        {error && <div className={styles.errorBox}>{error}</div>}

                        {activeSection === "users"
                            ? renderTable(userRows, "users")
                            : renderTable(trainerRows, "trainers")}
                    </div>
                )}

                {activeSection === "content" && (
                    <div className={styles.section}>
                        <h2>Content Management</h2>
                        <p>Content management functionality will be implemented here</p>
                        <div className={styles.placeholder}>
                            <p>• View all uploaded content</p>
                            <p>• Moderate content</p>
                            <p>• Content analytics</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
