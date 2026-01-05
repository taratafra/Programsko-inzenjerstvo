import styles from "./Questionnaire.module.css";

const PasswordResetModal = ({ onPasswordReset, passwordResetData, onPasswordChange }) => {
    const handleSubmit = (e) => {
        e.preventDefault();
        onPasswordReset(e);
    };

    const handleChange = (e) => {
        onPasswordChange(e);
    };

    return (
        <div className={styles.modalOverlay}>
            <div className={styles.modalContent}>
                <h2>Password Reset Required</h2>
                <p>
                    This is your first login. You must set a new password before continuing to the questionnaire.
                </p>

                <form onSubmit={handleSubmit}>
                    <div className={styles.newPass}>
                        <label>New Password:</label>
                        <input
                            type="password"
                            name="newPassword"
                            value={passwordResetData.newPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            placeholder="Enter new password (min 8 characters)"
                        />
                    </div>

                    <div className={styles.confirmPass}>
                        <label>Confirm Password:</label>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={passwordResetData.confirmPassword}
                            onChange={handleChange}
                            required
                            minLength="8"
                            placeholder="Confirm new password"
                        />
                    </div>
                    <button className={styles.passwordResetSubmit} type="submit">
                        Set New Password & Continue
                    </button>
                </form>
                <p className={styles.cannotContinue}>
                    You cannot continue to the questionnaire until you set a new password.
                </p>
            </div>
        </div>
    );
};

export default PasswordResetModal;
