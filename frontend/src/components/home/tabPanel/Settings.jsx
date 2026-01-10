import HomeStyles from "../../../pages/Home/Home.module.css"; // koristi isti CSS kao tabPanel
import styles from "./Settings.module.css"

import { useState, useEffect} from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

//password:
import PasswordResetModal from "./PasswordResetModal";

const EXPERIENCE_MAP = {  //ok ako backend vraca BEGINNER
  BEGINNER: "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED: "advanced"
};

export default function Settings({user}) {
    const navigate = useNavigate();
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    
    const [loading, setLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");
    
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [passwordResetData, setPasswordResetData] = useState({
        newPassword: "",
        confirmPassword: ""
    });

    
    const [form, setForm] = useState({
        name:"",
        surname:"",
        stress: 3,
        sleep: 3,
        experience: "",
        goals: [],
        sessionLength: "",
        preferredTime: "",
        notes: "",
    });

    /*auth token*/
    const getToken = async () => {
    const localToken = localStorage.getItem("token");

    if (isAuthenticated) {
      return getAccessTokenSilently({
        authorizationParams: {
          audience: BACKEND_URL,
          scope: "openid profile email"
        }
      });
    }

    if (localToken) return localToken;
    throw new Error("Not authenticated");
  };

  /*ucitaj postojece podatke*/
    useEffect(() => {
        const loadData = async () => {
            try {
                const token = await getToken();
                const res = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {////////////provjeri
                    headers: { Authorization: `Bearer ${token}` }
            });

            const data = res.ok? await res.json(): {};

            setForm({
                name:user?.name || "",
                surname:user?.surname || "",
                stress: data.stress ?? 3,
                sleep: data.sleep ?? 3,
                experience: EXPERIENCE_MAP[data.experience] || "",
                goals: data.goals ?? [],
                sessionLength: data.sessionLength ?? "",
                preferredTime: data.preferredTime ?? "",
                notes: data.notes ?? "",

                });
            } catch (err) {
                console.info("Faileed to load data.");
            } finally {
                setLoading(false);
            }
        };

        loadData();
    },[user]);

    
    const handleChange =(e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const toggleGoal = (goal) => {
        setForm((prev) => ({
        ...prev,
        goals: prev.goals.includes(goal)
            ? prev.goals.filter((g)=> g !== goal)
            : [...prev.goals, goal]
        }));
    };

    const handlePasswordChange=(e) => {
        const { name, value } = e.target;
        setPasswordResetData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    //
    const handlePasswordReset = async (e) => {
        e.preventDefault();

        if (passwordResetData.newPassword !== passwordResetData.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        try {
            const token = await getToken();

            const res = await fetch(`${BACKEND_URL}/auth/reset-password`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                newPassword: passwordResetData.newPassword
            })
            });

            if (!res.ok) throw new Error();

            // zatvori modal i resetiraj state
            setShowPasswordModal(false);
            setPasswordResetData({ newPassword: "", confirmPassword: "" });
        } catch {
            setError("Password reset failed.");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError("");

        try {
            const token = await getToken();

            const payload = {
                stress: Number(form.stress),
                sleep: Number(form.sleep),
                experience: Object.keys(EXPERIENCE_MAP)
                .find(key => EXPERIENCE_MAP[key] === form.experience),
                goals: form.goals,
                notes: form.notes, ///tu sam dodala var.
                sessionLength: form.sessionLength,
                preferredTime: form.preferredTime
            };

            const res = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error("Save failed");

            navigate("/home", { replace: true });
        } catch {
        setError("Failed to save settings.");
        } finally {
        setIsSubmitting(false);
        }
    };

    const handleClosePasswordModal=()=>{
        setShowPasswordModal(false);
        setPasswordResetData({ newPassword: "", confirmPassword: "" });
        setError("");
    }

    if (loading) return <div>Loading...</div>;

    return(     
        <div className={HomeStyles.tabPanel}>
            <form onSubmit={handleSubmit} id="onboarding-form">
                <fieldset className={styles.basicInfo}>
                    <legend>Basic information</legend>
                    <div>
                        <label htmlFor="name">Name: {""}</label>
                        <input 
                            id="name" 
                            type="text" 
                            value={form.name}
                            autoComplete="name" 
                        />
                    </div>

                    <div>
                        <label htmlFor="surname">Surname</label>
                        <input 
                            id="surname" 
                            name="surname" 
                            type="text" 
                            value={form.surname}
                            autoComplete="surname" />
                    </div>
                </fieldset>

                <fieldset className={styles.security}>
                    <legend>Security</legend>

                    <button
                        type="button"
                        className={styles.settbutton} 
                        onClick={() => setShowPasswordModal(true)}
                    >
                        Change password
                    </button>
                </fieldset>

                <fieldset className={styles.wellbeing}>
                    <legend>Wellbeing</legend>
                    <div>
                        <label htmlFor="stress">Stress level (1–5) <span aria-hidden="true">*</span></label>
                        <div className={styles.inputWrapper}>
                            <span className={styles.helpLeft}>No stress</span>
                            <input
                                id="stress"
                                name="stress"
                                type="range"
                                min="1"
                                max="5"
                                step="1"
                                defaultValue="3"
                                required
                                aria-describedby="stress-help"
                                value={form.stress}
                                onChange={handleChange}
                            />
                            <span className={styles.helpRight}>Extremely high stress</span>
                        </div>

                    </div>

                    <div>
                        <label htmlFor="sleep">Sleep quality (1–5) <span aria-hidden="true">*</span></label>
                        <div className={styles.inputWrapper}>
                            <span className={styles.helpLeft}>Very poor</span>
                            <input
                                id="sleep"
                                name="sleep"
                                type="range"
                                min="1"
                                max="5"
                                step="1"
                                defaultValue="3"
                                required
                                aria-describedby="sleep-help"
                                value={form.sleep}
                                onChange={handleChange}
                            />
                            <span className={styles.helpRight}>Excellent</span>
                        </div>

                    </div>

                </fieldset>

                <fieldset className={styles.meditationExp}>
                    <legend>Meditation experience <span aria-hidden="true">*</span></legend> 
                    {["beginner", "intermediate", "advanced"].map((lvl) => (
                        <label key={lvl}>
                            <input 
                                type="radio"
                                name="experience" 
                                value={lvl} 
                                required 
                                checked={form.experience === lvl}
                                onChange={handleChange}
                                /> {lvl}
                        </label>
                    ))}
                </fieldset>

                <fieldset className={styles.goals}>
                    <legend>Your goals (select all that apply)</legend>
                    {[
                        "reduce-anxiety",
                        "improve-sleep",
                        "increase-focus",
                        "stress-management",
                        "build-habit"
                        ].map((g) => (
                        <label key={g}>
                            <input 
                                type="checkbox" 
                                name="goals" 
                                checked={form.goals.includes(g)}
                                onChange={() => toggleGoal(g)}
                            /> {g}
                        </label>
                    ))}
                </fieldset>

                <fieldset className={styles.practice}>
                    <legend>Practice preferences</legend>
                    <div>
                        <label htmlFor="preferred-time">Preferred time of day</label>
                        <select 
                            id="preferred-time"
                            name="preferredTime" 
                            value={form.preferredTime}
                            onChange={handleChange}>
                            <option value="" disabled>Choose…</option>
                            <option value="morning">Morning</option>
                            <option value="afternoon">Afternoon</option>
                            <option value="evening">Evening</option>
                            <option value="flexible">Flexible</option>
                        </select>
                    </div>

                    <div>
                        <label htmlFor="session-length">Ideal session length</label>
                        <select 
                           id="session-length" 
                           name="sessionLength" 
                           value={form.sessionLength}
                           onChange={handleChange}>
                            <option value="" disabled>Choose…</option>
                            <option value="5-10">5–10 minutes</option>
                            <option value="10-15">10–15 minutes</option>
                            <option value="15-20">15–20 minutes</option>
                            <option value="20-plus">20+ minutes</option>
                        </select>
                    </div>

                    <div>
                        <label htmlFor="notes">Anything we should know?</label>
                        <textarea
                            id="notes"
                            name="notes"
                            placeholder="Share context (triggers, constraints...)"
                            rows={5}
                            value={form.notes}
                            onChange={handleChange}
                        />
                    </div>
                </fieldset>
                
                {error && <p className={styles.error}>{error}</p>}
                
                <button className={styles.settbutton}  type="submit" disabled={isSubmitting}>
                    {isSubmitting ? "Saving…" : "Save changes"}
                </button>
                {/* <div className={styles.buttonRow}>
                    <button className={styles.submitGoals} type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Generating..." : "Generate my plan"}
                    </button>
                    <button className={styles.resetGoals} type="reset">Reset</button>
                </div>

                <p>Fields marked with * are required.</p> */}
            </form>

            {showPasswordModal && (
                <PasswordResetModal
                    onPasswordReset={handlePasswordReset}
                    passwordResetData={passwordResetData}
                    onPasswordChange={handlePasswordChange}
                    onClose={handleClosePasswordModal}
                />
            )}
 
        </div>
)}