// src/Login.jsx
import { useState, useEffect } from "react";
import './login.css';
import CloudBackground from "./components/backgrounds/CloudyBackground";
import WhiteRectangle from "./components/backgrounds/WhiteRectangle.jsx";
import { Link, useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

function Login() {
  const [email, setEmail] = useState(""); // use email for clarity
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const BACKEND_URL = process.env.REACT_APP_BACKEND; // example: http://localhost:8080
  const { loginWithRedirect, isAuthenticated, isLoading } = useAuth0();
  const navigate = useNavigate();

  // Redirect to home if Auth0 is already authenticated
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate("/home");
    }
  }, [isAuthenticated, isLoading, navigate]);

   // Handle local login
  const handleLogin = async (e) => {
    e.preventDefault();
    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(`${BACKEND_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, socialLogin: false }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Login failed");
      }

      const data = await response.json();
      localStorage.setItem("token", data.access_token);

      setMessage("Login successful!");
      navigate("/home");
    } catch (error) {
      console.error("Error:", error);
      setMessage(error.message || "Failed to connect to backend");
    } finally {
      setLoading(false);
    }
  };

  if (isLoading) return <div>Loading...</div>;

  return (
    <CloudBackground>
      <WhiteRectangle>
        <p className="LOGIN">LOGIN</p>
        <div className="register-step-content1">
          <form className="forma" onSubmit={handleLogin}>
            <div className="email">
              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="password">
              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="submit-btn" disabled={loading}>
              {loading ? "Logging in..." : "Login"}
            </button>

            {message && <p className="error">{message}</p>}
          </form>

          <div className="separator">
            <span>or</span>
          </div>

          <button
            className="google-btn"
            onClick={() =>
              loginWithRedirect({
                appState: { returnTo: "/home" },
                // Optional: include audience if needed
                // authorizationParams: { audience: process.env.REACT_APP_AUTH0_AUDIENCE }
              })
            }
          >
            Login with Google / Other options
          </button>

          <div className="alternativa">
            <p>
              Don’t have an account? <Link to="/register">Register</Link>
            </p>
          </div>
        </div>
      </WhiteRectangle>
    </CloudBackground>
  );
}

export default Login;
