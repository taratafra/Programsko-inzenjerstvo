//ne koristi se
import React from "react"; 

function NameSurname({ name, surname, onChange, errors }) {     
    return (         
        <div className="register-step-content">             
            <p>Let's start with your name</p>             
            {/* Name input */}             
            <div className="username">                 
                <input type="text" placeholder="Name" value={name} onChange={(e) => onChange("name", e.target.value)} required/>             
            </div>             
            {errors?.name && (                 
                <div className="register-error">{errors.name}</div>             
            )}             
            {/* Surname input */}             
            <div className="username">                 
                <input type="text" placeholder="Surname" value={surname} onChange={(e) => onChange("surname", e.target.value)} required/>             
            </div>             
            {errors?.surname && (                 
                <div className="register-error">
                    {errors.surname}
                </div>             
            )}         
        </div>     
    ); 
} 

export default NameSurname;
