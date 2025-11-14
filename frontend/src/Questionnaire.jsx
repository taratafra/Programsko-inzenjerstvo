import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

import "./Questionnaire.css";
import CloudBackground from "./components/backgrounds/CloudyBackground";

const PasswordResetModal = ({ onPasswordReset, passwordResetData, onPasswordChange }) => {
    const handleSubmit = (e) => {
        e.preventDefault();
        onPasswordReset(e);
    };

    const handleChange = (e) => {
        onPasswordChange(e);
    };

    return (

        <div className="Prvi">
            <div className="Drugi">
                <h2>
                    Password Reset Required
                </h2>
                <p>
                    This is your first login. You must set a new password before continuing to the questionnaire.
                </p>

                <form onSubmit={handleSubmit}>
                    <div className="newPass">
                        <label>
                            New Password:
                        </label>
                        <input
                            type="password"
                            name="newPassword"
                            value={passwordResetData.newPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            placeholder="Enter new password (min 8 characters)"
                        />
                    </div>

                    <div className="confirmPass">
                        <label>
                            Confirm Password:
                        </label>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={passwordResetData.confirmPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            placeholder="Confirm new password"
                        />
                    </div>
                    <button ClassName="PasswordResetSubmit" type="submit">
                        Set New Password & Continue
                    </button>
                </form>
                <p className="cannotContinue">
                    You cannot continue to the questionnaire until you set a new password.
                </p>
            </div>
        </div>
    );
};

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
    const AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE;

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
                    if (userData.firstLogin)
                    {
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError("");

        try {
            const formData = new FormData(e.target);
            const consentChecked = formData.get("consent");
            if (!consentChecked) {
                setError("You must agree to the consent terms before generating your plan.");
                setIsSubmitting(false);
                return;
            }
            let token;
            const localToken = localStorage.getItem("token");

            if (isAuthenticated) {
                token = await getAccessTokenSilently({
                    authorizationParams: {
                        audience: AUDIENCE,
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

            const otherGoal = formData.get("goal-other");
            let noteText = formData.get("notes") || "";
            if (otherGoal && otherGoal.trim()) {
                noteText = noteText ? `${noteText}\n\nOther goal: ${otherGoal.trim()}` : `Other goal: ${otherGoal.trim()}`;
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

            const surveyData = {
                stressLevel: parseInt(formData.get("stress")),
                sleepQuality: parseInt(formData.get("sleep")),
                meditationExperience: experienceMapping[experience],
                goals: goals, 
                note: noteText
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
        <div className="upitnik_svi">
            <form onSubmit={handleSubmit} id="onboarding-form" method="post" noValidate>
                <h1 className="Onboarding">Onboarding & Goals</h1>
                <p>Please fill out this short questionnaire so we can generate a personalized 7-day practice plan.</p>

                {error && (
                    <div className="errortext">
                        <p>{error}</p>
                    </div>
                )}

                <fieldset className="BasicInfo">
                    <legend>Basic information</legend>

                    <div>
                        <label htmlFor="name">Full name</label>
                        <input id="name" name="name" type="text" placeholder="e.g., Ana Horvat" autoComplete="name" />
                    </div>

                    <div>
                        <label htmlFor="email">Email</label>
                        <input id="email" name="email" type="email" placeholder="e.g., ana@example.com" autoComplete="email" />
                    </div>
                </fieldset>

                <fieldset className="Wellbeing">
                    <legend>Wellbeing</legend>

                    <div>
                        <label htmlFor="stress">Stress level (1–5) <span aria-hidden="true">*</span></label>
                        <div className="input-wrapper">
                            <span id="help-left">No stress</span>
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
                            <span id="help-right">Extremely high stress</span>
                        </div>
                        
                    </div>

                    <div>
                        <label htmlFor="sleep">Sleep quality (1–5) <span aria-hidden="true">*</span></label>
                        <div className="input-wrapper">
                            <span id="help-left">Very poor</span>
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
                            <span id="help-right">Excellent</span>
                        </div>
                        
                    </div>

                </fieldset>

                <fieldset className="MeditationExp">
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

                <fieldset className="Goals">
                    <legend>Your goals (select all that apply)</legend>

                    <div>
                        <label>
                            <input type="checkbox" name="goals" value="reduce-anxiety" /> Reduce anxiety
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
                    <div className="otherGoal">
                        <label htmlFor="goal-other">Other goal:</label>
                        <input id="goal-other" name="goal-other" type="text" placeholder="Describe another goal" />
                    </div>
                </fieldset>

                <fieldset className="Practice">
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

                <fieldset className="Consent">
                    <legend>Consent</legend>
                    <label>
                        <input type="checkbox" name="consent" value="agree" required /> I agree that my responses will be used to generate a personalized 7-day plan. 
                        <span aria-hidden="true">*</span>
                    </label>
                </fieldset>


                <div className="button-row">
                    <button className="submitGoals" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Generating..." : "Generate my plan"}
                    </button>
                    <button className="resetGoals" type="reset">Reset</button>
                </div>

                <p>Fields marked with * are required.</p>
            </form>
        </div>
    );

    if (loading) return <div>Loading...</div>;

    return (
        <CloudBackground>  
            <div className="password_tab">
                {requiresPasswordReset && (
                    <PasswordResetModal
                        onPasswordReset={handlePasswordReset}
                        passwordResetData={passwordResetData}
                        onPasswordChange={handlePasswordChange}
                    />
                )}
            
                <div style={requiresPasswordReset ? { filter: 'blur(5px)', pointerEvents: 'none' } : {}}>
                    {renderQuestionnaireForm()}
                </div>
            </div>            
            </CloudBackground>

    );
}
