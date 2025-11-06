import { Navigate, Outlet } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

const ProtectedRoute = () => { //ovo treba popraviti ne radi 
    const { isAuthenticated, isLoading } = useAuth0();

    if (isLoading) {
        return <div>Loading...</div>;
    }

    return isAuthenticated ? <Outlet/> : <Navigate to='/login'/>

}

export default ProtectedRoute;