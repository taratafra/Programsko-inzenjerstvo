//ne koristi se

import React from "react"; 

function AccountType({ accountType, onChange, errors }) {     
    return (         
        <div className="register-step-content">             
            <p>What type of account would you like to create?</p>             
            <div className="register-account-type">                 
                <label className="account-option">                     
                    <input type="radio" name="accountType" value="user" checked={accountType === "user"} 
                        onChange={(e) => onChange("accountType", e.target.value)}/>                     
                    <span>User</span>                 
                </label>                 
                <label className="account-option">                     
                    <input type="radio" name="accountType" value="trainer" checked={accountType === "trainer"}
                        onChange={(e) => onChange("accountType", e.target.value)}/>                     
                    <span>Trainer</span>                 
                </label>             
            </div>             
            {errors?.accountType && (                 
                <div className="register-error">{errors.accountType}</div>)}         
        </div>     
    ); 
} 

export default AccountType;
