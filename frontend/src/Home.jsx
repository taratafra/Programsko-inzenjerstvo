import { useAuth0 } from "@auth0/auth0-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Home() {
    const { user, getAccessTokenSilently, isLoading } = useAuth0();
    const [responseFromServer, setResponse] = useState('');
    const navigate = useNavigate();

    console.log("Home - Current user:", user);
    console.log("Home - isLoading:", isLoading);


    useEffect(() => {
        if (user) {
            console.log("User found, fetching protected data");
            sendUserDataToBackend();  
            getDataFromResourceServer();
        }
    }, [user]);

      const sendUserDataToBackend = async () => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: 'http://localhost:8080',
                    scope: "openid profile email",
                }
            });

            const payload = {
                name: user.given_name || user.name?.split(" ")[0] || "",
                surname: user.family_name || user.name?.split(" ")[1] || "",
                email: user.email,
                lastLogin: new Date().toISOString(),
                isSocialLogin: true,
                auth0Id: user.sub,
            };

            console.log("Sending user data:", payload);

            const response = await fetch("http://localhost:8080/api/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                console.error("Failed to send user data:", response.statusText);
            } else {
                console.log("User data sent successfully");
            }
        } catch (error) {
            console.error("Error sending user data to backend:", error);
        }
    };

    const getDataFromResourceServer = async () => {
        try {
            console.log("token");
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: 'http://localhost:8080',
                    scope: "openid profile email",
                }
            });
            const response = await fetch("http://localhost:8080/protected", {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
            const responseData = await response.text();
            console.log(responseData);
            setResponse(responseData);
        } catch (error) {
            console.error("Error fetching protected data:", error);
        }
    }


    if (isLoading) {
        return <div>Loading...</div>;
    }

    if (!user) {
        return <div>No user found...</div>;
    }

    

    return (
        <div>
            <h1>Login Successful!</h1>
            <h2>Welcome, {user.name}!</h2>
            <h3>{responseFromServer}</h3>
            <p>Email: {user.email}</p>
        </div>
    );
}