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
      const data = Object.fromEntries(formData);
      
      // uzimamo token
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

      
      // saljemo na backend 
      const response = await fetch(`${BACKEND_URL}/api/users/complete-onboarding`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error("Failed to complete onboarding");
      }

      console.log("Onboarding completed successfully!");
      
      // vracamo ljude na home
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
          <label htmlFor="stress">Stress level (0–10) <span aria-hidden="true">*</span></label>
          <input
            id="stress"
            name="stress"
            type="range"
            min="0"
            max="10"
            step="1"
            defaultValue="5"
            required
            aria-describedby="stress-help"
          />
          <div id="stress-help">0 = no stress, 10 = extremely high stress</div>
        </div>

        <div>
          <label htmlFor="sleep">Sleep quality <span aria-hidden="true">*</span></label>
          <select id="sleep" name="sleep" required defaultValue="">
            <option value="" disabled>Choose…</option>
            <option value="very-poor">Very poor</option>
            <option value="poor">Poor</option>
            <option value="average">Average</option>
            <option value="good">Good</option>
            <option value="excellent">Excellent</option>
          </select>
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