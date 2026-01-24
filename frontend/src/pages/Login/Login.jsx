import { useState, useEffect } from "react";
//import styles from './Login.module.css';
import './login.css';
import CloudBackground from "../../components/backgrounds/CloudyBackground";
import WhiteRectangle from "../../components/backgrounds/WhiteRectangle.jsx";
import { Link, useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { FcGoogle } from "react-icons/fc";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const BACKEND_URL = process.env.REACT_APP_BACKEND;
  const { loginWithRedirect, isAuthenticated, isLoading, getAccessTokenSilently } = useAuth0();
  const navigate = useNavigate();

  useEffect(() => {
    const checkAuthAndRedirect = async () => {
      if (!isLoading && isAuthenticated) {
        // Check if user is a trainer before redirecting to home
        try {
          const token = await getAccessTokenSilently({
            authorizationParams: {
              audience: process.env.REACT_APP_AUTH0_AUDIENCE,
              scope: "openid profile email",
            },
          });

          // Get user data from backend
          const userRes = await fetch(`${BACKEND_URL}/api/users`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({
              email: "", // Will be populated from token
              isSocialLogin: true,
            }),
          });

          if (userRes.ok) {
            const userData = await userRes.json();

            // If user is a trainer, check approval status
            if (userData.role === "TRAINER") {
              const trainerRes = await fetch(`${BACKEND_URL}/api/trainers/me`, {
                headers: { Authorization: `Bearer ${token}` },
              });

              if (trainerRes.ok) {
                const trainerData = await trainerRes.json();
                
                // Redirect based on approval status
                if (!trainerData.approved) {
                  navigate("/trainer-lobby");
                  return;
                }
              } else if (trainerRes.status === 404) {
                // Trainer record doesn't exist
                navigate("/trainer-lobby");
                return;
              }
            }
          }
        } catch (error) {
          console.error("Error checking user status:", error);
        }

        // If all checks pass, redirect to home
        navigate("/home");
      }
    };

    checkAuthAndRedirect();
  }, [isAuthenticated, isLoading, navigate, BACKEND_URL, getAccessTokenSilently]);

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

      let data;
      const text = await response.text();
      try {
        data = JSON.parse(text);
      } catch {
        data = { message: text };
      }

      if (!response.ok) {

        if (response.status === 403 || 
            data.error === "UserBanned" || 
            data.message?.toLowerCase().includes("banned") ||
            data.message?.toLowerCase().includes("account has been suspended")) {
          alert("Your account has been banned. Please contact support for more information.");
          throw new Error("Your account has been banned");
        }

        if (response.status === 404 || response.status === 500 && data.message?.toLowerCase().includes("user not found")) {
          throw new Error("Email does not exist");
        }
        if (response.status === 401 || data.error === "InvalidPassword" || data.message?.toLowerCase().includes("invalid password")) {
          throw new Error("Incorrect Password");
        }
        throw new Error(data.error || data.message || "Login failed. Please try again.");
      }
      if (data.access_token) {
        localStorage.setItem("token", data.access_token);
      }

      // Check if user is a trainer and if they're approved
      if (data.user && data.user.role === "TRAINER") {
        try {
          const trainerRes = await fetch(`${BACKEND_URL}/api/trainers/me`, {
            headers: { Authorization: `Bearer ${data.access_token}` },
          });

          if (trainerRes.ok) {
            const trainerData = await trainerRes.json();
            
            // If trainer is not approved, redirect to trainer lobby
            if (!trainerData.approved) {
              setMessage("Your trainer account is pending approval.");
              navigate("/trainer-lobby");
              return;
            }
          }
        } catch (error) {
          console.error("Error checking trainer status:", error);
          // If we can't verify trainer status, redirect to lobby to be safe
          navigate("/trainer-lobby");
          return;
        }
      }

      setMessage("Login successful!");
      navigate("/home");
    } catch (error) {
      console.error("Login error:", error);
      setMessage(error.message);
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
              })
            }
          >
            <FcGoogle className="FcGoogle" size={20} />Login with Google / Other options
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
