import { useEffect, useMemo, useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import styles from "./AdminDashboard.module.css";

export default function AdminDashboard({ setActiveTab }) {
    const [activeSection, setActiveSection] = useState("overview");

    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    const [users, setUsers] = useState([]);
    const [trainers, setTrainers] = useState([]);
    const [pendingTrainers, setPendingTrainers] = useState([]);
    const [videos, setVideos] = useState([]);
    const [currentAdmin, setCurrentAdmin] = useState(null);

    // Stats
    const [stats, setStats] = useState({
        totalUsers: 0,
        totalTrainers: 0,
        activeSessions: 0,
        totalContent: 0,
        pendingApprovals: 0,
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

    const normalizeRole = (role) => (role || "").toString().trim().toUpperCase();

    const getIsBanned = (obj) => {
        if (!obj) return false;
        if (typeof obj.isBanned === "boolean") return obj.isBanned;
        if (typeof obj.banned === "boolean") return obj.banned;
        if (typeof obj.is_banned === "boolean") return obj.is_banned;
        if (typeof obj.ban === "boolean") return obj.ban;
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

    // Fetch current admin info
    const fetchCurrentAdmin = async () => {
        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/admins/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (res.ok) {
                const data = await res.json();
                setCurrentAdmin(data);
            }
        } catch (e) {
            console.error("fetchCurrentAdmin error:", e);
        }
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
            // Filter out admins from the user list
            const filteredUsers = Array.isArray(data) 
                ? data.filter(user => normalizeRole(user?.role) !== "ADMIN")
                : [];
            setUsers(filteredUsers);
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

    const fetchPendingTrainers = async () => {
        setLoading(true);
        setError("");
        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/admins/trainers/pending`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to fetch pending trainers (${res.status})`);
            }

            const data = await res.json();
            setPendingTrainers(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("fetchPendingTrainers error:", e);
            setError(e?.message || "Failed to load pending trainers");
            setPendingTrainers([]);
        } finally {
            setLoading(false);
        }
    };

    const fetchVideos = async () => {
        setLoading(true);
        setError("");
        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/videos?size=1000`, {
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to fetch videos (${res.status})`);
            }

            const data = await res.json();
            // Handle paginated response
            const videoList = data.content || data;
            setVideos(Array.isArray(videoList) ? videoList : []);
        } catch (e) {
            console.error("fetchVideos error:", e);
            setError(e?.message || "Failed to load videos");
            setVideos([]);
        } finally {
            setLoading(false);
        }
    };

    // Fetch stats for overview
    const fetchStats = async () => {
        setLoading(true);
        try {
            const token = await getToken();

            const [usersRes, trainersRes, videosRes, pendingRes] = await Promise.all([
                fetch(`${BACKEND_URL}/api/users`, {
                    headers: { Authorization: `Bearer ${token}` },
                }),
                fetch(`${BACKEND_URL}/api/trainers`, {
                    headers: { Authorization: `Bearer ${token}` },
                }),
                fetch(`${BACKEND_URL}/api/videos?size=1000`, {
                    headers: { Authorization: `Bearer ${token}` },
                }),
                fetch(`${BACKEND_URL}/api/admins/trainers/pending`, {
                    headers: { Authorization: `Bearer ${token}` },
                }),
            ]);

            const usersData = await usersRes.json();
            const trainersData = await trainersRes.json();
            const videosData = await videosRes.json();
            const pendingData = await pendingRes.json();

            // Filter out admins when counting users
            const usersList = Array.isArray(usersData) 
                ? usersData.filter(u => normalizeRole(u?.role) !== "ADMIN")
                : [];
            const trainersList = Array.isArray(trainersData) ? trainersData : [];
            const videosList = videosData.content || videosData;
            const pendingList = Array.isArray(pendingData) ? pendingData : [];

            // Calculate active users (non-banned users, excluding admins)
            const activeUsers = usersList.filter((u) => !getIsBanned(u)).length;

            setStats({
                totalUsers: usersList.length,
                totalTrainers: trainersList.length,
                activeSessions: activeUsers,
                totalContent: Array.isArray(videosList) ? videosList.length : 0,
                pendingApprovals: pendingList.length,
            });
        } catch (e) {
            console.error("fetchStats error:", e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCurrentAdmin();
    }, []);

    useEffect(() => {
        if (activeSection === "overview") fetchStats();
        if (activeSection === "users") fetchUsers();
        if (activeSection === "trainers") fetchTrainers();
        if (activeSection === "approvals") fetchPendingTrainers();
        if (activeSection === "content") fetchVideos();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeSection]);

    const matchesSearch = (item, q) => {
        const haystack = [
            item?.id,
            item?.email,
            item?.name,
            item?.surname,
            item?.role,
            item?.title,
            item?.description,
            item?.trainerName,
        ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();
        return haystack.includes(q);
    };

    const userRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return users
            .filter((u) => normalizeRole(u?.role) !== "TRAINER")
            .filter((u) => (q ? matchesSearch(u, q) : true));
    }, [users, search]);

    const trainerRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return trainers.filter((t) => (q ? matchesSearch(t, q) : true));
    }, [trainers, search]);

    const pendingTrainerRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return pendingTrainers.filter((t) => (q ? matchesSearch(t, q) : true));
    }, [pendingTrainers, search]);

    const videoRows = useMemo(() => {
        const q = search.trim().toLowerCase();
        return videos.filter((v) => (q ? matchesSearch(v, q) : true));
    }, [videos, search]);

    const refreshActive = async () => {
        if (activeSection === "overview") return fetchStats();
        if (activeSection === "users") return fetchUsers();
        if (activeSection === "trainers") return fetchTrainers();
        if (activeSection === "approvals") return fetchPendingTrainers();
        if (activeSection === "content") return fetchVideos();
    };

    const setBanStatus = async (id, banned, listType) => {
        // Prevent self-ban for users/trainers
        if (currentAdmin && currentAdmin.id === id && !banned) {
            // Allow self-unban (shouldn't happen, but just in case)
        } else if (currentAdmin && currentAdmin.id === id && banned) {
            alert("You cannot ban yourself!");
            return;
        }

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

            if (listType === "users") {
                setUsers((prev) =>
                    prev.map((u) =>
                        u.id === id
                            ? {
                                  ...u,
                                  isBanned: banned,
                                  banned,
                                  is_banned: banned,
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

    const approveTrainer = async (trainerId) => {
        try {
            const token = await getToken();
            const res = await fetch(
                `${BACKEND_URL}/api/admins/trainers/${trainerId}/approve`,
                {
                    method: "PATCH",
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to approve trainer (${res.status})`);
            }

            // Remove from pending list
            setPendingTrainers((prev) => prev.filter((t) => t.id !== trainerId));
            
            // Refresh stats
            fetchStats();
            
            alert("Trainer approved successfully!");
        } catch (e) {
            console.error("approveTrainer error:", e);
            alert(e?.message || "Failed to approve trainer");
        }
    };

    const rejectTrainer = async (trainerId) => {
        if (!window.confirm("Are you sure you want to reject this trainer application?")) {
            return;
        }

        try {
            const token = await getToken();
            const res = await fetch(
                `${BACKEND_URL}/api/admins/trainers/${trainerId}/reject`,
                {
                    method: "PATCH",
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to reject trainer (${res.status})`);
            }

            // Remove from pending list
            setPendingTrainers((prev) => prev.filter((t) => t.id !== trainerId));
            
            // Refresh stats
            fetchStats();
            
            alert("Trainer application rejected.");
        } catch (e) {
            console.error("rejectTrainer error:", e);
            alert(e?.message || "Failed to reject trainer");
        }
    };

    const deleteVideo = async (videoId) => {
        if (!window.confirm("Are you sure you want to delete this video?")) {
            return;
        }

        try {
            const token = await getToken();
            const res = await fetch(`${BACKEND_URL}/api/videos/${videoId}`, {
                method: "DELETE",
                headers: { Authorization: `Bearer ${token}` },
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || `Failed to delete video (${res.status})`);
            }

            setVideos((prev) => prev.filter((v) => v.id !== videoId));
            alert("Video deleted successfully!");
        } catch (e) {
            console.error("deleteVideo error:", e);
            alert(e?.message || "Failed to delete video");
        }
    };

    const renderUserTable = (rows, listType) => (
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
                            const isCurrentAdmin = currentAdmin && currentAdmin.id === u.id;

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
                                                disabled={isCurrentAdmin}
                                            >
                                                Unban
                                            </button>
                                        ) : (
                                            <button
                                                className={`${styles.actionBtn} ${styles.banBtn}`}
                                                onClick={() => setBanStatus(u.id, true, listType)}
                                                disabled={isCurrentAdmin}
                                                title={
                                                    isCurrentAdmin
                                                        ? "You cannot ban yourself"
                                                        : ""
                                                }
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

    const renderPendingTrainersTable = (rows) => (
        <div className={styles.tableWrap}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Applied</th>
                        <th className={styles.actionsCol}>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {!loading &&
                        rows.map((t) => {
                            const fullName =
                                `${t?.name || ""} ${t?.surname || ""}`.trim() || "-";
                            const createdAt = t.createdAt 
                                ? new Date(t.createdAt).toLocaleDateString()
                                : "-";

                            return (
                                <tr key={t.id}>
                                    <td>{t.id}</td>
                                    <td>{fullName}</td>
                                    <td>{t.email || "-"}</td>
                                    <td>{createdAt}</td>
                                    <td className={styles.actionsCol}>
                                        <button
                                            className={`${styles.actionBtn} ${styles.approveBtn}`}
                                            onClick={() => approveTrainer(t.id)}
                                        >
                                            Approve
                                        </button>
                                        <button
                                            className={`${styles.actionBtn} ${styles.rejectBtn}`}
                                            onClick={() => rejectTrainer(t.id)}
                                        >
                                            Reject
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}

                    {!loading && rows.length === 0 && (
                        <tr>
                            <td colSpan={5} className={styles.emptyRow}>
                                No pending trainer applications.
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    );

    const renderVideoTable = (rows) => (
        <div className={styles.tableWrap}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Type</th>
                        <th>Duration</th>
                        <th>Trainer</th>
                        <th className={styles.actionsCol}>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {!loading &&
                        rows.map((v) => {
                            const trainerName = v.trainerName || 
                                (v.trainer ? `${v.trainer.name || ""} ${v.trainer.surname || ""}`.trim() : "-");

                            return (
                                <tr key={v.id}>
                                    <td>{v.id}</td>
                                    <td>{v.title || "-"}</td>
                                    <td>
                                        <span className={styles.typeBadge}>
                                            {v.type || "-"}
                                        </span>
                                    </td>
                                    <td>{v.duration ? `${v.duration} min` : "-"}</td>
                                    <td>{trainerName}</td>
                                    <td className={styles.actionsCol}>
                                        <button
                                            className={`${styles.actionBtn} ${styles.deleteBtn}`}
                                            onClick={() => deleteVideo(v.id)}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}

                    {!loading && rows.length === 0 && (
                        <tr>
                            <td colSpan={6} className={styles.emptyRow}>
                                No content found.
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
                    className={activeSection === "approvals" ? styles.active : ""}
                    onClick={() => setActiveSection("approvals")}
                >
                    Trainer Approvals
                    {stats.pendingApprovals > 0 && (
                        <span className={styles.badge}>{stats.pendingApprovals}</span>
                    )}
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
                        <div className={styles.sectionHeaderRow}>
                            <h2>System Overview</h2>
                            <button
                                className={styles.refreshBtn}
                                onClick={fetchStats}
                                disabled={loading}
                            >
                                {loading ? "Loading..." : "Refresh"}
                            </button>
                        </div>
                        <div className={styles.statsGrid}>
                            <div className={styles.statCard}>
                                <div className={styles.statIcon}>👥</div>
                                <h3>Total Users</h3>
                                <p className={styles.statNumber}>
                                    {loading ? "..." : stats.totalUsers}
                                </p>
                            </div>
                            <div className={styles.statCard}>
                                <div className={styles.statIcon}>🎯</div>
                                <h3>Total Trainers</h3>
                                <p className={styles.statNumber}>
                                    {loading ? "..." : stats.totalTrainers}
                                </p>
                            </div>
                            <div className={styles.statCard}>
                                <div className={styles.statIcon}>✨</div>
                                <h3>Active Users</h3>
                                <p className={styles.statNumber}>
                                    {loading ? "..." : stats.activeSessions}
                                </p>
                            </div>
                            <div className={styles.statCard}>
                                <div className={styles.statIcon}>🎥</div>
                                <h3>Total Content</h3>
                                <p className={styles.statNumber}>
                                    {loading ? "..." : stats.totalContent}
                                </p>
                            </div>
                            <div className={styles.statCard}>
                                <div className={styles.statIcon}>⏳</div>
                                <h3>Pending Approvals</h3>
                                <p className={styles.statNumber}>
                                    {loading ? "..." : stats.pendingApprovals}
                                </p>
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
                                        ? "Manage user accounts (admins and trainers excluded)"
                                        : "Manage trainer accounts"}
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
                            ? renderUserTable(userRows, "users")
                            : renderUserTable(trainerRows, "trainers")}
                    </div>
                )}

                {activeSection === "approvals" && (
                    <div className={styles.section}>
                        <div className={styles.sectionHeaderRow}>
                            <div>
                                <h2>Trainer Approval Queue</h2>
                                <p className={styles.subtle}>
                                    Review and approve trainer applications
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
                                placeholder="Search by name, email, id…"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>

                        {error && <div className={styles.errorBox}>{error}</div>}

                        {renderPendingTrainersTable(pendingTrainerRows)}
                    </div>
                )}

                {activeSection === "content" && (
                    <div className={styles.section}>
                        <div className={styles.sectionHeaderRow}>
                            <div>
                                <h2>Content Management</h2>
                                <p className={styles.subtle}>
                                    Manage all videos, audios, and content in the system
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
                                placeholder="Search by title, type, trainer…"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>

                        {error && <div className={styles.errorBox}>{error}</div>}

                        <div className={styles.contentStats}>
                            <div className={styles.miniStat}>
                                <span className={styles.miniStatLabel}>Total Videos:</span>
                                <span className={styles.miniStatValue}>{videos.length}</span>
                            </div>
                        </div>

                        {renderVideoTable(videoRows)}
                    </div>
                )}
            </div>
        </div>
    );
}