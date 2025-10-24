
import { useState } from "react";
import './login.css';


function Login() {
    const [username, setUsername]=useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage]=useState("");
    const BACKEND_URL = process.env.REACT_APP_BACKEND
  const handleClick = async (e) => {
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

    return (
      <div className="sve">
        <div className="clouds">
          <div id="o1"></div>
          <div id="o2"></div>
          <div id="o3"></div>
        </div>
        <div className="pravokutnik">
          <p>LOGIN</p>
          <form action="/login" method="post">
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
                  required
                  />
              </div>
              
              <button type="submit" className="submit-btn">Login</button>
          </form>
          <div className="alternativa">
            <p>Dont have an account? 
            <a href="/register"> Register</a>
            </p>
          </div>
          
        </div>
      </div>
      
  );
}

export default Login;



