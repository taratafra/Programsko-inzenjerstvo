// Questionnaire.jszx
import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

const PasswordResetModal = ({ onPasswordReset, passwordResetData, onPasswordChange }) => {
    const handleSubmit = (e) => {
        e.preventDefault();
        onPasswordReset(e);
    };

    const handleChange = (e) => {
        onPasswordChange(e);
    };

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000,
        }}>
            <div style={{
                backgroundColor: 'white',
                padding: '30px',
                borderRadius: '15px',
                boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)',
                width: '90%',
                maxWidth: '500px',
                textAlign: 'center'
            }}>
                <h2 style={{ color: '#e74c3c', marginBottom: '15px' }}>
                    Password Reset Required
                </h2>
                <p style={{ marginBottom: '25px', color: '#555' }}>
                    This is your first login. You must set a new password before continuing to the questionnaire.
                </p>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '20px', textAlign: 'left' }}>
                        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
                            New Password:
                        </label>
                        <input
                            type="password"
                            name="newPassword"
                            value={passwordResetData.newPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            style={{
                                width: '100%',
                                padding: '12px',
                                borderRadius: '8px',
                                border: '2px solid #ddd',
                                fontSize: '16px'
                            }}
                            placeholder="Enter new password (min 8 characters)"
                        />
                    </div>

                    <div style={{ marginBottom: '25px', textAlign: 'left' }}>
                        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
                            Confirm Password:
                        </label>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={passwordResetData.confirmPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            style={{
                                width: '100%',
                                padding: '12px',
                                borderRadius: '8px',
                                border: '2px solid #ddd',
                                fontSize: '16px'
                            }}
                            placeholder="Confirm new password"
                        />
                    </div>

                    <button
                        type="submit"
                        style={{
                            backgroundColor: '#27ae60',
                            color: 'white',
                            padding: '12px 30px',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold',
                            width: '100%'
                        }}
                    >
                        Set New Password & Continue
                    </button>
                </form>

                <p style={{
                    marginTop: '15px',
                    fontSize: '12px',
                    color: '#777',
                    fontStyle: 'italic'
                }}>
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

    useEffect(() => {
        const checkPasswordResetRequirement = async () => {
            try {
                const localToken = localStorage.getItem("token");

                if (!localToken) {
                    // No local token means it's Auth0 user, no password reset needed
                    setRequiresPasswordReset(false);
                    setLoading(false);
                    return;
                }

                // Check if password reset is required for local users
                const userRes = await fetch(`${BACKEND_URL}/api/users/me`, {
                    headers: { Authorization: `Bearer ${localToken}` },
                });

                if (userRes.ok) {
                    const userData = await userRes.json();
                    if (userData.requiresPasswordReset || userData.firstLogin) {
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

            const otherGoal = formData.get("goal-other");
            let noteText = formData.get("notes") || "";
            if (otherGoal && otherGoal.trim()) {
                noteText = noteText ? `${noteText}\n\nOther goal: ${otherGoal.trim()}` : `Other goal: ${otherGoal.trim()}`;
            }

            // Map meditation experience to enum
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
                goals: goals, // Backend expects Set<Goal> enum as array
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
        <div style={{
            maxWidth: "800px",
            margin: "0 auto",
            padding: "20px",
            position: 'relative'
        }}>
            <form onSubmit={handleSubmit} id="onboarding-form" method="post" noValidate>
                <h1>Onboarding & Goals</h1>
                <p>Please fill out this short questionnaire so we can generate a personalized 7-day practice plan.</p>

                {error && (
                    <div style={{ padding: '1rem', backgroundColor: '#fee', border: '1px solid #fcc', borderRadius: '4px', marginBottom: '1rem' }}>
                        <p style={{ color: '#c00' }}>{error}</p>
                    </div>
                )}

                <fieldset>
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

                <fieldset>
                    <legend>Wellbeing</legend>

                    <div>
                        <label htmlFor="stress">Stress level (1–5) <span aria-hidden="true">*</span></label>
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
                        <div id="stress-help">1 = no stress, 5 = extremely high stress</div>
                    </div>

                    <div>
                        <label htmlFor="sleep">Sleep quality (1–5) <span aria-hidden="true">*</span></label>
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
                        <div id="sleep-help">1 = Very poor, 2 = Poor, 3 = Average, 4 = Good, 5 = Excellent</div>
                    </div>

                </fieldset>

                {/* Meditation experience */}
                <fieldset>
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

                <fieldset>
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
                    <div>
                        <label htmlFor="goal-other">Other goal</label>
                        <input id="goal-other" name="goal-other" type="text" placeholder="Describe another goal (optional)" />
                    </div>
                </fieldset>

                <fieldset>
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
                            placeholder="Optional: share context (e.g., triggers, constraints, past practices you liked)"
                            rows={5}
                        />
                    </div>
                </fieldset>

                <fieldset>
                    <legend>Consent</legend>
                    <label>
                        <input type="checkbox" name="consent" value="agree" required /> I agree that my responses will be used to generate a personalized 7-day plan. <span aria-hidden="true">*</span>
                    </label>
                </fieldset>


                <div>
                    <button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Generating..." : "Generate my plan"}
                    </button>
                    <button type="reset">Reset</button>
                </div>

                <p>Fields marked with * are required.</p>
            </form>
        </div>
    );

    if (loading) return <div>Loading...</div>;

    return (
        <div style={{ position: 'relative', minHeight: '100vh', padding: '20px' }}>
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
    );
}