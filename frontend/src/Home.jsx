// src/Home.jsx
import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Home() { 

  const { user: auth0User, getAccessTokenSilently, isLoading, isAuthenticated } = useAuth0();
  const [user, setUser] = useState(null);
  const [responseFromServer, setResponse] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const AUTH0_AUDIENCE = process.env.REACT_APP_AUTH0_AUDIENCE; 
  const BACKEND_URL = process.env.REACT_APP_BACKEND;
  const getApiToken = async (localToken) => {
    if (localToken) return localToken; // local login
    // Auth0 access token
    return await getAccessTokenSilently({
      authorizationParams: {
        audience: `${AUTH0_AUDIENCE}`,
        scope: "openid profile email",
      },
    });
  };

const checkFirstLogin = async (token) => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/users/me`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
      console.log(res);
      if (!res.ok) return false;
      const me = await res.json();
      return !!me?.isFirstLogin;
    } catch {
      return false;
    }
  };

      useEffect(() => {
    const init = async () => {
      const localToken = localStorage.getItem("token");

      try { 
        // Auth0 login
        console.log("Ave maria");
        if (isAuthenticated && auth0User) {
          console.log("Authenticated via Auth0:", auth0User);
          setUser(auth0User);
          const token = await getApiToken(null);
          const isFirst = await checkFirstLogin(token);
          if(isFirst){
            navigate("/questions");
            return;
          }

          await sendUserDataToBackend(auth0User);
          await fetchProtectedResource(); // SDK provides token internally
        } 
        //  Local JWT login
        else if (localToken) {
          console.log("Authenticated via local JWT");
            
          // Fetch user info from backend
          const res = await fetch(`${BACKEND_URL}/api/users/me`, {
            headers: { Authorization: `Bearer ${localToken}` },
          });

          if (!res.ok) throw new Error("Failed to fetch user info");

          const data = await res.json();
          setUser(data);
          const isFirst = await checkFirstLogin(localToken);
            console.log("Is first token:", isFirst);
            if (isFirst) {
                navigate("/questions");
                return;
            }
          // Fetch protected resource with local token
          await fetchProtectedResource(localToken);
        } 
        setLoading(false);
      } catch (err) {
            console.error("Error initializing user:", err);
            setLoading(false);
      }
    };

    init();
  }, [auth0User, isAuthenticated, navigate, BACKEND_URL]);

  //  Send Auth0 user info to backend
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

      console.log("Sending user payload:", payload);

      const res = await fetch(`${BACKEND_URL}/api/users`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        console.error("Failed to send user data:", res.statusText);
      } else {
        console.log("User data synced successfully with backend");
      }
    } catch (err) {
      console.error("Error sending user data to backend:", err);
    }
  };

  //  Fetch protected resource (works for both Auth0 and local JWT)
  const fetchProtectedResource = async (localToken) => {
    try {
      let token;

      if (localToken) {
        token = localToken; // local JWT
      } else {
        // Auth0 token
        token = await getAccessTokenSilently({
          authorizationParams: {
            audience: `${BACKEND_URL}`,
            scope: "openid profile email",
          },
        });
      }

      const res = await fetch(`${BACKEND_URL}/protected`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) throw new Error("Failed to fetch protected resource");

      const data = await res.text();
      setResponse(data);
    } catch (err) {
      console.error("Error fetching protected resource:", err);
      setResponse("Error fetching protected resource");
    }
  };

  // 🔹 Render logic
  if (loading || isLoading) return <div>Loading...</div>;
  if (!user) return <div>No user found...</div>;

  return (
    <div>
      <h1>Login Successful!</h1>
      <h2>Welcome, {user.name || user.given_name || user.email}!</h2>
      <p>Email: {user.email}</p>
      {responseFromServer && <h3>{responseFromServer}</h3>}
    </div>
  );
}
