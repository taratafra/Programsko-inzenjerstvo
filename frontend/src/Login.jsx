import { useState, useEffect } from "react";
import './login.css';
import CloudBackground from "./components/backgrounds/CloudyBackground";
import WhiteRectangle from "./components/backgrounds/WhiteRectangle.jsx"
import { FcGoogle } from "react-icons/fc";
import {Link, useNavigate} from "react-router-dom"
import { useAuth0 } from "@auth0/auth0-react"; /////////////////////minjala//////////////////////////////



function Login() {
    const [username, setUsername]=useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const BACKEND_URL = process.env.REACT_APP_BACKEND;
    const {loginWithRedirect, user, isAuthenticated, isLoading, error} = useAuth0();// ovo sam mijenjala///////////////////////////////////////////
    const navigate = useNavigate();


    useEffect(() => {
    
    // mijenjalaaa
        if (!isLoading && isAuthenticated) {
            console.log("Redirecting to home from Login");
            navigate("/home");
        }
    }, [isAuthenticated, isLoading, navigate]);


    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch('${BACKEND_URL}/login',{//ne postoji dok oni zabusavaju
                method: "POST",
        //tu dodat nesto za pretrazivanje backenda? predlozeno ovo dolje ali nez sto znaci
                headers:{
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({username, password}),
            });
            if(!response.ok){
                throw new Error("Login failed");
            }
            const data = await response.text();
            setMessage(`Succes: ${data}`);
        } catch (error) {
            console.error("Error:", error);
            setMessage("Failed to connect to backend");
        }
    };

    //minjalaa
    if (isLoading) {
        return (
            <div>Loading...</div>
        )
    }


    return (
        <CloudBackground>
            <WhiteRectangle>
                <p>LOGIN</p>
                <form onSubmit={handleLogin}>
                    <div className="username">
                        <input 
                            type="text" 
                            placeholder="Username" 
                            value={username}
                            onChange={(e)=>setUsername(e.target.value)}
                            required/>
                    </div>
                    <div className="password">
                        <input 
                            type="password" 
                            placeholder="Password"
                            value={password}
                            onChange={(e)=>setPassword(e.target.value)}
                            required/>
                    </div>

                    <button type="submit" className="submit-btn">Login</button>
                </form>

                <div className="separator">
                    <span>or</span>
                </div>

                <button className="google-btn" onClick={() => {
                        
                    loginWithRedirect({
                                appState: { returnTo: "/home" }
                            })}}>Login with Google / Other options</button>


                <div className="alternativa">
                    <p>Dont have an account? 
                        <Link to="/register">Register</Link>
                    </p>
                </div>
            </WhiteRectangle>
        </CloudBackground>
    );
}

export default Login;



