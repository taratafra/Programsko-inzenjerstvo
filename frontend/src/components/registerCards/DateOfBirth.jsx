import React from "react"; 

function DateOfBirth({ dateOfBirth, onChange, errors }) {     
    return (         
        <div className="register-step-content">             
            <p>When were you born?</p>             
            <div className="username">                 
                <input type="date" value={dateOfBirth} onChange={(e) => onChange("dateOfBirth", e.target.value)} required/>             
            </div>             

            {errors?.DateOfBirth && (                 
                <div className="register-error">{errors.dob}</div>             
            )}         
        </div>     
    ); 
} 

export default DateOfBirth; 
