import styles from "../../../pages/Questionnaire/Questionnaire.module.css";

const PasswordResetModal = ({ onPasswordReset, passwordResetData, onPasswordChange, onClose }) => {
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
                <h2>Change password</h2>
                <form onSubmit={handleSubmit}>
                    <div className={styles.currentPass}>
                        <label htmlFor="currentPassword">Current Password</label>
                        <input
                            type="password"
                            id="currentPassword"
                            name="currentPassword"
                            value={passwordResetData.currentPassword}
                            onChange={onPasswordChange}
                            required
                        />
                    </div>

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
                    <button 
                        className={styles.passwordResetSubmit} 
                        type="submit">
                            Set New Password & Continue
                    </button>
                    <button 
                        className={styles.quit} 
                        type="button" 
                        onClick={onClose}>
                            Quit
                    </button>
                </form>
            </div>
        </div>
    );
};


export default PasswordResetModal;
