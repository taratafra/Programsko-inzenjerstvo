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
            getDataFromResourceServer();
        }
    }, [user]);

    const getDataFromResourceServer = async () => {
        try {
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: 'http://localhost:8080',
                    scope: "read:current_user",
                }
            });
            const response = await fetch("http://localhost:8080/protected", {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
            const responseData = await response.text();
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