import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

import styles from "./Questionnaire.module.css";
import CloudBackground from "../../components/backgrounds/CloudyBackground";
import PasswordResetModal from "./PasswordResetModal";

export default function Questionnaire() {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [requiresPasswordReset, setRequiresPasswordReset] = useState(false);
    const [passwordResetData, setPasswordResetData] = useState({
        newPassword: "",
        confirmPassword: ""
    });
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";

    useEffect(() => {
        const checkPasswordResetRequirement = async () => {
            try {
                const localToken = localStorage.getItem("token");

                if (!localToken) {
                    setRequiresPasswordReset(false);
                    setLoading(false);
                    return;
                }

                const userRes = await fetch(`${BACKEND_URL}/api/users/me`, {
                    headers: { Authorization: `Bearer ${localToken}` },
                });


                if (userRes.ok) {
                    const userData = await userRes.json();
                    console.log(userData)
                    if (userData.firstLogin) {
                        setRequiresPasswordReset(true);
                    } else {
                        setRequiresPasswordReset(false);
                    }
                }
                setLoading(false);
            } catch (err) {
                console.error("Error checking password requirement:", err);
                setRequiresPasswordReset(false);
                setLoading(false);
            }
        };

        checkPasswordResetRequirement();
    }, [BACKEND_URL]);

    const handlePasswordReset = useCallback(async (e) => {
        e.preventDefault();

        if (passwordResetData.newPassword !== passwordResetData.confirmPassword) {
            alert("Passwords don't match!");
            return;
        }

        if (passwordResetData.newPassword.length < 8) {
            alert("Password must be at least 8 characters long!");
            return;
        }

        try {
            const localToken = localStorage.getItem("token");
            const res = await fetch(`${BACKEND_URL}/api/user/settings/first-time-reset`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localToken}`,
                },
                body: JSON.stringify({
                    newPassword: passwordResetData.newPassword
                }),
            });

            if (res.ok) {
                setRequiresPasswordReset(false);
                alert("Password reset successfully! You can now continue with the questionnaire.");
                setPasswordResetData({ newPassword: "", confirmPassword: "" });
            } else {
                const error = await res.text();
                alert(`Password reset failed: ${error}`);
            }
        } catch (err) {
            console.error("Error resetting password:", err);
            alert("Error resetting password. Please try again.");
        }
    }, [passwordResetData, BACKEND_URL]);

    const handlePasswordChange = useCallback((e) => {
        const { name, value } = e.target;
        setPasswordResetData(prev => ({
            ...prev,
            [name]: value
        }));
    }, []);

    ///submit: 

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError("");

        try {
            const formData = new FormData(e.target);

            let token;
            const localToken = localStorage.getItem("token");

            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: {
                        audience: BACKEND_URL,
                        scope: "openid profile email",
                    },
                });
            } else if (localToken) {
                token = localToken;
            } else {
                setError("Not authenticated");
                setIsSubmitting(false);
                return;
            }

            const goalMapping = {
                "reduce-anxiety": "REDUCE_ANXIETY",
                "improve-sleep": "IMPROVE_SLEEP",
                "increase-focus": "INCREASE_FOCUS",
                "stress-management": "STRESS_MANAGEMENT",
                "build-habit": "BUILD_HABIT"
            };

            const goals = [];
            formData.getAll("goals").forEach(goal => {
                const enumValue = goalMapping[goal];
                if (enumValue) {
                    goals.push(enumValue);
                }
            });

            if(goals.length===0){//dodano
                setError("Please select at least one goal");
                setIsSubmitting(false);
                return;
            }

            const experienceMapping = {
                "beginner": "BEGINNER",
                "intermediate": "INTERMEDIATE",
                "advanced": "ADVANCED"
            };

            const experience = formData.get("experience");
            if (!experience) {
                setError("Please select your meditation experience level");
                setIsSubmitting(false);
                return;
            }

            const roleMapping ={
                "user":"USER",
                "coach":"COACH"
            }

            const role=formData.get("role");
            if(!role){
                setError("Please select your role");
                setIsSubmitting(false);
                return;
            }

            const consent =formData.get("consent");
            if (!consent) {
                setError("You must give consent to continue.");
                setIsSubmitting(false);
                return;
            }
            

            const surveyData = {
                stressLevel: parseInt(formData.get("stress")),
                sleepQuality: parseInt(formData.get("sleep")),
                meditationExperience: experienceMapping[experience],
                goals: goals,
                sessionLength: formData.get("session-length") || null,   
                preferredTime: formData.get("preferred-time") || null,   
                note: formData.get("notes") || null 
            };

            console.log("Submitting survey data:", surveyData);

            const surveyResponse = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(surveyData)
            });

            if (!surveyResponse.ok) {
                const errorText = await surveyResponse.text();
                console.error("Survey submission error:", {
                    status: surveyResponse.status,
                    statusText: surveyResponse.statusText,
                    body: errorText
                });

                if (surveyResponse.status === 409) {
                    console.log("Survey exists, attempting update...");
                    const updateResponse = await fetch(`${BACKEND_URL}/onboarding/survey/me`, {
                        method: "PUT",
                        headers: {
                            "Authorization": `Bearer ${token}`,
                            "Content-Type": "application/json",
                        },
                        body: JSON.stringify(surveyData)
                    });

                    if (!updateResponse.ok) {
                        const updateErrorText = await updateResponse.text();
                        console.error("Survey update error:", {
                            status: updateResponse.status,
                            statusText: updateResponse.statusText,
                            body: updateErrorText
                        });
                        throw new Error(`Failed to update survey: ${updateResponse.status} - ${updateErrorText}`);
                    }
                } else {
                    throw new Error(`Failed to submit survey: ${surveyResponse.status} - ${errorText}`);
                }
            }

            const onboardingResponse = await fetch(`${BACKEND_URL}/api/users/complete-onboarding`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });

            if (!onboardingResponse.ok) {
                throw new Error("Failed to complete onboarding");
            }

            const updatedUser = await onboardingResponse.json();
            console.log("Onboarding completed successfully!", updatedUser);

            navigate("/home", { replace: true });

        } catch (err) {
            console.error("Error submitting questionnaire:", err);
            setError("Failed to submit questionnaire. Please try again.");
            setIsSubmitting(false);
        }
    };

    // upitnik
    const renderQuestionnaireForm = () => (
        <div className={styles.upitnikSvi}>
            <form onSubmit={handleSubmit} id="onboarding-form" method="post" noValidate>
                <h1 className={styles.onboarding}>Onboarding & Goals</h1>
                <p>Please fill out this short questionnaire so we can generate a personalized 7-day practice plan.</p>

                {error && (
                    <div className={styles.errortext}>
                        <p>{error}</p>
                    </div>
                )}

                <fieldset className={styles.role}>
                    <legend>Role<span aria-hidden="true">*</span></legend>
                    <div>
                        <label htmlFor="role">What type of account would you like to create?</label>
                        <div>
                            <label>
                                <input type="radio" name="role" value="user" required /> User
                            </label>
                        </div>
                        <div>
                            <label>
                                <input type="radio" name="role" value="coach" /> Coach
                            </label>
                        </div>
                    </div>
                    

                    
                </fieldset>
                <fieldset className={styles.wellbeing}>
                    <legend>Wellbeing<span aria-hidden="true">*</span></legend>

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
                            />
                            <span className={styles.helpRight}>Excellent</span>
                        </div>

                    </div>

                </fieldset>

                <fieldset className={styles.meditationExp}>
                    <legend>Meditation experience <span aria-hidden="true">*</span></legend>

                    <div>
                        <label>
                            <input type="radio" name="experience" value="beginner" required /> Beginner
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="radio" name="experience" value="intermediate" /> Intermediate
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="radio" name="experience" value="advanced" /> Advanced
                        </label>
                    </div>
                </fieldset>

                <fieldset className={styles.goals}>
                    <legend>Your goals (select all that apply)<span aria-hidden="true">*</span></legend>

                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="reduce-anxiety"/> Reduce anxiety
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="improve-sleep" /> Improve sleep
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="increase-focus" /> Increase focus
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="stress-management" /> Better stress management
                        </label>
                    </div>
                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="build-habit" /> Build a meditation habit
                        </label>
                    </div>
                </fieldset>

                <fieldset className={styles.practice}>
                    <legend>Practice preferences</legend>

                    <div>
                        <label htmlFor="preferred-time">Preferred time of day</label>
                        <select id="preferred-time" name="preferred-time" defaultValue="">
                            <option value="" disabled>Choose…</option>
                            <option value="morning">Morning</option>
                            <option value="afternoon">Afternoon</option>
                            <option value="evening">Evening</option>
                            <option value="flexible">Flexible</option>
                        </select>
                    </div>

                    <div>
                        <label htmlFor="session-length">Ideal session length</label>
                        <select id="session-length" name="session-length" defaultValue="">
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
                        />
                    </div>
                </fieldset>

                <fieldset className={styles.consent}>
                    <legend>Consent<span aria-hidden="true">*</span></legend>
                    <label>
                        <input type="checkbox" name="consent" value="agree" required /> I agree that my responses will be used to generate a personalized 7-day plan.
                    </label>
                </fieldset>


                <div className={styles.buttonRow}>
                    <button className={styles.submitGoals} type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Generating..." : "Generate my plan"}
                    </button>
                    <button className={styles.resetGoals} type="reset">Reset</button>
                </div>

                <p>Fields marked with * are required.</p>
            </form>
        </div>
    );

    if (loading) return <div>Loading...</div>;

    return (
        <CloudBackground>
            <div className={styles.passwordTab}>
                {requiresPasswordReset && (
                    <PasswordResetModal
                        onPasswordReset={handlePasswordReset}
                        passwordResetData={passwordResetData}
                        onPasswordChange={handlePasswordChange}
                    />
                )}

                <div className={requiresPasswordReset ? styles.formBlurred : ''}>
                    {renderQuestionnaireForm()}
                </div>
            </div>
        </CloudBackground>

    );
}
