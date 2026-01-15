//ne koristi se
import React from "react";
import styles from "./FinalizeGoogle.module.css";

function FinalizeGoogle({ googleError, onContinueToLogin }) {
    return (
        <div className={`register-step-content ${styles.stepContent}`}>
            <p>Your account has been set up using Google.</p>
            <p>You can now continue to login.</p>
            {googleError && (
                <div className={`register-error ${styles.errorContainer}`}>
                    {googleError}
                </div>
            )}
            <div className={styles.buttonContainer}>
                <button type="button" className="submit-btn" onClick={onContinueToLogin}>
                    Continue to login
                </button>
            </div>
        </div>
    );
}
export default FinalizeGoogle;
