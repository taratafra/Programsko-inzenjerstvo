import React from "react"; 

function FinalizeGoogle({ googleError, onContinueToLogin }) {     
    return (         
        <div className="register-step-content" style={{ textAlign: "center" }}>             
            <p>Your account has been set up using Google.</p>             
            <p>You can now continue to login.</p>             
            {googleError && (                 
                <div className="register-error" style={{ marginTop: "1rem" }}>                     
                    {googleError}                 
                </div>             
            )}             
            <div style={{ marginTop: "2rem" }}>                 
                <button type="button" className="submit-btn" onClick={onContinueToLogin}>               
                    Continue to login                 
                </button>             
            </div>         
        </div>     
    ); 
} 
export default FinalizeGoogle;
