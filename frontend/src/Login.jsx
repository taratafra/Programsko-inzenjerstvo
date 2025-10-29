import { useState } from "react";
import './login.css';
import CloudBackground from "./components/backgrounds/CloudyBackground";
import { FcGoogle } from "react-icons/fc";


function Login() {
    const [username, setUsername]=useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const BACKEND_URL = process.env.REACT_APP_BACKEND;
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

    const handleGoogleLogin = async (e) => {
        console.log("Attempting to login with google");
        //Ovo je funkcija koja će rukovat loginom preko googla al nam backend ekipa
        //mora javit šta se vamo treba dogodit zasad se samo logga 
    };

    return (
        <CloudBackground>
            <div className="pravokutnik">
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
                
                <button type="button" className="google-btn" onClick={handleGoogleLogin}>
                    <FcGoogle size={22} /> Login with Google
                </button>
                <div className="alternativa">
                    <p>Dont have an account? 
                        <a href="/register"> Register</a>
                    </p>
                </div>
            </div>
        </CloudBackground>
    );
}

export default Login;



