// Questionnaire.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

export default function Questionnaire() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();
  
  const BACKEND_URL = process.env.REACT_APP_BACKEND || "http://localhost:8080";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError("");

    try {
      const formData = new FormData(e.target);
      
      // Get token (either from Auth0 or localStorage)
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

      // Process goals - collect all checked checkboxes and map to enum values
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
      
      // If user added a custom goal in "other", we can add it as a note
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

      // Prepare survey data matching OnboardingSurveyRequest DTO
      const surveyData = {
        stressLevel: parseInt(formData.get("stress")),
        sleepQuality: parseInt(formData.get("sleep")),
        meditationExperience: experienceMapping[experience],
        goals: goals, // Backend expects Set<Goal> enum as array
        note: noteText
      };

      console.log("Submitting survey data:", surveyData);

      // Submit questionnaire to backend
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
          // Survey already exists, try updating instead
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

      // Mark onboarding as complete
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
      
      // Navigate to home
      navigate("/home", { replace: true });
      
    } catch (err) {
      console.error("Error submitting questionnaire:", err);
      setError("Failed to submit questionnaire. Please try again.");
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} id="onboarding-form" method="post" noValidate>
      <h1>Onboarding & Goals</h1>
      <p>Please fill out this short questionnaire so we can generate a personalized 7-day practice plan.</p>

      {error && (
        <div style={{ padding: '1rem', backgroundColor: '#fee', border: '1px solid #fcc', borderRadius: '4px', marginBottom: '1rem' }}>
          <p style={{ color: '#c00' }}>{error}</p>
        </div>
      )}

      {/* Basic info */}
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

      {/* Stress & sleep */}
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

      {/* Goals */}
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

      {/* Preferences */}
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

      {/* Consent */}
      <fieldset>
        <legend>Consent</legend>
        <label>
          <input type="checkbox" name="consent" value="agree" required /> I agree that my responses will be used to generate a personalized 7-day plan. <span aria-hidden="true">*</span>
        </label>
      </fieldset>

      {/* Submit */}
      <div>
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Generating..." : "Generate my plan"}
        </button>
        <button type="reset">Reset</button>
      </div>

      <p>Fields marked with * are required.</p>
    </form>
  );
}