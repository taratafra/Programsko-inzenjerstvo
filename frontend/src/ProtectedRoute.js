import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

const ProtectedRoute = () => { 
    //ovaj branch protection radi samo za usere koji su autentificirani preko auth0-a
    const { isAuthenticated, isLoading } = useAuth0();
    const location = useLocation();
    const hasLocalToken = !!localStorage.getItem("token");

    if (isLoading && !hasLocalToken) {
        return <div>Loading...</div>;
    }

    if ( isAuthenticated || hasLocalToken ) {
        return <Outlet />;

    return isAuthenticated ? <Outlet/> : <Navigate to='/login'/>

    }
}

export default ProtectedRoute;
