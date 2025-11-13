function EmailPassword({ email, password, rePassword, onChange, errors, onRegister }) {     
 /*   react.useEffect(()=> {   
        onChange("password","test123");
    }, [onChange]);tu gotovo*/

    return (         
        <div className="register-step-content">  

            <p>Enter your email</p>           
            <div className="username">                 
                <input type="email" placeholder="Email" value={email} onChange={(e) => onChange("email", e.target.value)} required/>
            </div>             
            {errors?.email && (                 
                <div className="register-error">{errors.email}</div>             
            )}                          
            {/* <div className="password">                 
                <input type="password" placeholder="Password" value={password} onChange={(e) => onChange("password", e.target.value)} required/>
            </div>             
            {errors?.password && (                 
                <div className="register-error">
                    {errors.password}
                </div>
            )}
            <div className="password">                 
                <input type="password" placeholder="Confirm password" value={rePassword} onChange={(e) => onChange("rePassword", e.target.value)} required/>             
            </div>             
            {errors?.rePassword && (                 
                <div className="register-error">
                    {errors.rePassword}
                </div>             
            )}          */
           <p>Your temporary password: test123   <br/><br/>
           COPY IT FOR LATER USE <br/><br/></p>//ovo treba generirat na backu i boldat lozinku
           }
 
            <button type="button" className="submit-btn" onClick={onRegister}>Register</button>
        </div>     
    ); 
} 

export default EmailPassword; 
