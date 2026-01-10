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
    const { loginWithRedirect, isAuthenticated, isLoading } = useAuth0();
    const navigate = useNavigate();

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate("/home");
        }
    }, [isAuthenticated, isLoading, navigate]);

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
                if (response.status === 404 || response.status === 500 && data.message?.toLowerCase().includes("user not found")) {
                    throw new Error("Email does not exist");
                }
                if (response.status === 401 || data.error === "InvalidPassword" || data.message?.toLowerCase().includes("invalid password")) {
                    throw new Error("Incorrect Password");
                }
                throw new Error(data.error || data.message || "Login failed. Please try again.");
            }
            if (data.access_token) localStorage.setItem("token", data.access_token);

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
                <FcGoogle className= "FcGoogle" size={20} />Login with Google / Other options
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
    
/*
    return (
        <CloudBackground>
            <WhiteRectangle>
                <p className={styles.login}>LOGIN</p>
                <div className="register-step-content1">
                    <form className={styles.forma} onSubmit={handleLogin}>
                        <div className={styles.email}>
                            <input
                                type="email"
                                placeholder="Email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className={styles.password}>
                            <input
                                type="password"
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>

                        <button type="submit" className={styles.submitBtn} disabled={loading}>
                            {loading ? "Logging in..." : "Login"}
                        </button>

                        {message && <p className={styles.error}>{message}</p>}
                    </form>

                    <div className={styles.separator}>
                        <span>or</span>
                    </div>

                    <button
                        className={styles.googleBtn}
                        onClick={() =>
                            loginWithRedirect({
                                appState: { returnTo: "/home" },
                            })
                        }
                    >
                        Login with Google / Other options
                    </button>

                    <div className={styles.alternativa}>
                        <p>
                            Don't have an account? <Link to="/register">Register</Link>
                        </p>
                    </div>
                </div>
            </WhiteRectangle>
        </CloudBackground>
    );
}

export default Login;*/
