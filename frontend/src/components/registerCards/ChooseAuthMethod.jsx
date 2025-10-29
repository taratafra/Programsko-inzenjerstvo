import { FcGoogle } from "react-icons/fc";

function ChooseAuthMethod({setAuthMethod, error}) {
    return (         
        <div className="register-step-content">           
            <p>How would you like to create your account?</p>           
            <div className="register-auth-choice">             
                 <button type="button" className="submit-btn" onClick={() => setAuthMethod("email")}>
                    Use email &amp; password
                    </button>
                <div className="separator">               
                    <span>or</span>             
                </div>             
                             
                <button type="button" className="google-btn" onClick={() => setAuthMethod("google")}>               
                    <FcGoogle size={22} />Continue with Google             
                </button> 
            </div>           

            {error && (             
                <div className="register-error">{error}</div>           
            )}         
        </div>       
    );
}

export default ChooseAuthMethod;
