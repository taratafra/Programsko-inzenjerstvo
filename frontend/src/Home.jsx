// src/Home.jsx 
import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";

export default function Home() { 
  const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const initRan = useRef(false);

  const BACKEND_URL = process.env.REACT_APP_BACKEND;




  useEffect(() => {
    if (initRan.current) return;

    const init = async () => {
      const localToken = localStorage.getItem("token");

      try { 
        // Auth0 login preko Google ...
        if (isAuthenticated && auth0User) {
          initRan.current = true;
          setUser(auth0User);
          
          const userResponse = await sendUserDataToBackend(auth0User);
          
          if (userResponse?.isFirstLogin) {
            navigate("/questions", { replace: true });
            return;
          }

          setLoading(false);
        } 
        // Local JWT login
        else if (localToken) {
          initRan.current = true;
          
          const res = await fetch(`${BACKEND_URL}/api/users/me`, {
            headers: { Authorization: `Bearer ${localToken}` },
          });

          if (!res.ok) throw new Error("Failed to fetch user info");

          const data = await res.json();
          setUser(data);
          
          if (data.isFirstLogin) {
            navigate("/questions", { replace: true });
            return;
          }
          
          setLoading(false);
        } else {
          setLoading(false);
        }
      } catch (err) {
        console.error("Error initializing user:", err);
        setLoading(false);
      }
    };

    if (!isLoading) {
      init();
    }
  }, [isLoading, isAuthenticated]);




  const sendUserDataToBackend = async (auth0User) => {
    try {
      const token = await getAccessTokenSilently({
        authorizationParams: {
          audience: `${BACKEND_URL}`,
          scope: "openid profile email",
        },
      });

      const payload = {
        name: auth0User.given_name || auth0User.name?.split(" ")[0] || "",
        surname: auth0User.family_name || auth0User.name?.split(" ")[1] || "",
        email: auth0User.email,
        lastLogin: new Date().toISOString(),
        isSocialLogin: true,
        auth0Id: auth0User.sub,
      };

      const res = await fetch(`${BACKEND_URL}/api/users`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) return null;
      
      return await res.json();
    } catch (err) {
      console.error("Error sending user data to backend:", err);
      return null;
    }
  };




  if (loading || isLoading) return <div>Loading...</div>;
  if (!user) return <div>No user found...</div>;




  return (
    <div>
      <h1>Login Successful!</h1>
      <h2>Welcome, {user.name || user.given_name || user.email}!</h2>
      <p>Email: {user.email}</p>
    </div>
  );
}