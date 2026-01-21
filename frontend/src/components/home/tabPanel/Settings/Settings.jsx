import HomeStyles from "../../../../pages/Home/Home.module.css"; // koristi isti CSS kao tabPanel
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

export default function Settings({user, updateUser}) {
    const navigate = useNavigate();
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();
    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";
    
    const [loading, setLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState(""); 

    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [passwordResetData, setPasswordResetData] = useState({
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
    });

    
    const [form, setForm] = useState({
        name:"",
        surname:"",
        bio:"",
        profilePictureUrl:"",
        isSocialLogin: false,
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
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: BACKEND_URL,
                    scope: "openid profile email"
                }
            });

            return token
        }

        if (localToken) return localToken;
        throw new Error("Not authenticated");
    };

  /*ucitaj postojece podatke*/
    useEffect(() => {
        const loadData = async () => {
            try {
                const token = await getToken();
                
                const surveyRes = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {
                headers: { Authorization: `Bearer ${token}` }
                });
                const surveyData = surveyRes.ok ? await surveyRes.json() : {};

                setForm({
                    name:user?.name || "",
                    surname:user?.surname || "",
                    isSocialLogin: user.isSocialLogin,
                    stress: surveyData.stressLevel ?? 3,
                    sleep: surveyData.sleepQuality ?? 3,
                    experience: EXPERIENCE_MAP[surveyData.meditationExperience] || "",
                    goals: surveyData.goals 
                        ? surveyData.goals.map(g => g.toLowerCase().replace(/_/g, '-'))
                        :[],
                    sessionLength: surveyData.sessionLength ?? "",
                    preferredTime: surveyData.preferredTime ?? "",
                    notes: surveyData.note ?? "",
                });

            } catch (err) {
                console.error("Faileed to load data.");
                setError("Failed to load your settings.");
            } finally {
                setLoading(false);
            }
        };

        if(user){
           loadData(); 
        }  
    },[user]);

    
    const handleChange =(e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
        setError("");
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

        if (passwordResetData.newPassword.length < 8) {
            setError("Password must be at least 8 characters long.");
            return;
        }

        try {
            const token = await getToken();

            const res = await fetch(`${BACKEND_URL}/api/user/settings/change-password`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    currentPassword: passwordResetData.currentPassword,  // ADD THIS
                    newPassword: passwordResetData.newPassword
                })
            });

            if (!res.ok) throw new Error("Password reset failed");

            // zatvori modal i resetiraj state
            setShowPasswordModal(false);
            setPasswordResetData({ newPassword: "", confirmPassword: "" });
            setSuccessMessage("Password changed successfully!");
        } catch(err) {
            setError("Password reset failed.");
        }
    };

    const validateForm = () => {
        if (!form.name || !form.name.trim()) {
            setError("Name is required.");
            return false;
        }

        if (!form.surname || !form.surname.trim()) {
            setError("Surname is required.");
            return false;
        }

        if (!form.experience) {
            setError("Please select your meditation experience level.");
            return false;
        }

        if (form.goals.length === 0) {
            setError("Please select at least one goal.");
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError("");
        setSuccessMessage("");

        if(!validateForm()){
            setIsSubmitting(false);
            return;
        }

        try {
            const token = await getToken();

            // Update user basic info
            const userUpdateRes = await fetch(`${BACKEND_URL}/api/user/settings/profile`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name: form.name,
                    surname: form.surname,
                    bio: form.bio || null,
                    profilePictureUrl: form.profilePictureUrl || null
                })
            });

            if (!userUpdateRes.ok){
                throw new Error("Failed to update user info");
            }

            //ažuriraj user state u Home.jsx
            if (updateUser) {
                updateUser({
                    name: form.name,
                    surname: form.surname
                });
            }

            const surveyPayLoad = {
                stressLevel: Number(form.stress),
                sleepQuality: Number(form.sleep),
                meditationExperience: Object.keys(EXPERIENCE_MAP).find(key => EXPERIENCE_MAP[key] === form.experience),
                goals: form.goals.map(g => g.toUpperCase().replace(/-/g, '_')),
                note: form.notes,
                sessionLength: form.sessionLength,
                preferredTime: form.preferredTime
            };

            const res = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(surveyPayLoad)
            });

            if (!res.ok){
               throw new Error("Save failed"); 
            } 

            setSuccessMessage("Settings saved successfully!")
            navigate("/home", { replace: true });
        } catch(err) {
            setError("Failed to save settings.");
        } finally {
            setIsSubmitting(false);
        }
    };

        const handleClosePasswordModal = () => {
        setShowPasswordModal(false);
        setPasswordResetData({ currentPassword: "", newPassword: "", confirmPassword: "" }); // ADD currentPassword
        setError("");
    }

    if (loading){
        <div className={HomeStyles.tabPanel}>
            return <div>Loading...</div>; 
        </div>
    } 

    return(     
        <div className={HomeStyles.tabPanel}>
            <form onSubmit={handleSubmit} id="onboarding-form">
                <fieldset className={styles.basicInfo}>
                    <legend>Basic information</legend>
                    <div>
                        <label htmlFor="name">Name: {""}</label>
                        <input 
                            id="name" 
                            name="name"
                            type="text" 
                            value={form.name}
                            onChange={handleChange}
                            autoComplete="name" 
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="surname">Surname</label>
                        <input 
                            id="surname" 
                            name="surname" 
                            type="text" 
                            value={form.surname}
                            onChange={handleChange}
                            autoComplete="surname" 
                            required/>
                    </div>
                </fieldset>

                <fieldset className={styles.security}>
                    <legend>Security</legend>

                    {form.isSocialLogin ? (
                        <p className={styles.infoText}>
                            You are logged in through Google or something else that you selected. Password changes are managed through your social media account.
                        </p>
                    ) : (
                        <button
                            type="button"
                            className={styles.settbutton} 
                            onClick={() => setShowPasswordModal(true)}
                        >
                            Change password
                        </button>
                    )}
                </fieldset>

                <fieldset className={styles.wellbeing}>
                    <legend>Wellbeing <span aria-hidden="true">*</span></legend>
                    <div>
                        <label htmlFor="stress">Stress level (1–5)</label>
                        <div className={styles.inputWrapper}>
                            <span className={styles.helpLeft}>No stress</span>
                            <input
                                id="stress"
                                name="stress"
                                type="range"
                                min="1"
                                max="5"
                                step="1"
                                value={form.stress}
                                aria-describedby="stress-help"
                                onChange={handleChange}
                            />
                            <span className={styles.helpRight}>Extremely high stress</span>
                        </div>
                    </div>

                    <div>
                        <label htmlFor="sleep">Sleep quality (1–5)</label>
                        <div className={styles.inputWrapper}>
                            <span className={styles.helpLeft}>Very poor</span>
                            <input
                                id="sleep"
                                name="sleep"
                                type="range"
                                min="1"
                                max="5"
                                step="1"
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
                            /> {" "}{lvl.charAt(0).toUpperCase() + lvl.slice(1)}
                        </label>
                    ))}
                </fieldset>

                <fieldset className={styles.goals}>
                    <legend>Your goals (select all that apply)<span aria-hidden="true">*</span></legend>
                    {[
                        { value: "reduce-anxiety", label: "Reduce anxiety" },
                        { value: "improve-sleep", label: "Improve sleep" },
                        { value: "increase-focus", label: "Increase focus" },
                        { value: "stress-management", label: "Stress management" },
                        { value: "build-habit", label: "Build habit" }
                        ].map((g) => (
                        <label key={g.value}>
                            <input 
                                type="checkbox" 
                                name="goals" 
                                value={g.value}
                                checked={form.goals.includes(g.value)}
                                onChange={() => toggleGoal(g.value)}
                            /> {" "}{g.label}
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
                            onChange={handleChange}
                        >
                            <option value="">Choose…</option>
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
                            <option value="">Choose…</option>
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
                
                {error && <p className={styles.error} role="alert">{error}</p>}
                {successMessage && <p className={styles.success} role="status">{successMessage}</p>}
                
                <button className={styles.settbutton}  type="submit" disabled={isSubmitting}>
                    {isSubmitting ? "Saving…" : "Save changes"}
                </button>
                <p>Fields marked with * are required.</p>
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