import { useState } from "react";
import './Register.css';
import CloudBackground from "./components/backgrounds/CloudyBackground";
import WhiteRectangle from "./components/backgrounds/WhiteRectangle"
import ChooseAuthMethod from "./components/registerCards/ChooseAuthMethod"
import NameSurname from "./components/registerCards/NameSurname";
import DateOfBirth from "./components/registerCards/DateOfBirth";
import AccountType from "./components/registerCards/AccountType";
import FinalizeGoogle from "./components/registerCards/FinalizeGoogle";
import NameSurnameCard from "./components/registerCards/NameSurname";
import EmailPassword from "./components/registerCards/EmailPassword";
import { FcGoogle } from "react-icons/fc";
import { useNavigate } from "react-router-dom";

function Register() {
    const [currentStep, setCurrentStep]=useState(0);
    
    const navigate = useNavigate();

    const [regData, setRegData] = useState({
        authMethod: "",
        name: "",
        surname: "",
        dateOfBirth: "",
        accountType: "",
        email: "",
        password: "",
        rePassword: "",
        googleToken: "", //This will be important when google auth is enabled on backend

    });

    const [errors, setErrors] = useState({});

    const handleRegDataUpdate = (field, value) => {
        setRegData((prev)=> ({
            ...prev,
            [field]: value,
            }));
    };

    const setAuthMethod = async (method) => {
        if(method == "google") {
            //Here we will need to handle the inizialization of the google authentification
            //The two lines down there will be needed but right now are nonsense
            handleRegDataUpdate("authMethod", "google");
            handleRegDataUpdate("googleToken", "OVO_JE_IZMISLJENO"); 

        }else if(method == "email") {
            handleRegDataUpdate("authMethod", "email");
        }
        setCurrentStep(1);
    };

    const validateInputs = () => {
        const newErrors = {}

        //Nothing to check for step 0 

        if(currentStep == 1) {
            if(!regData.name.trim()) {
                newErrors.name = "Please enter your name";
            }

            if(!regData.surname.trim()) {
                newErrors.surname = "Please enter your surname";
            }
        }

        if(currentStep == 2 && !regData.dateOfBirth) {
            newErrors.dateOfBirth = "Please enter your date of birth";
        }

        if(currentStep == 3 && !regData.accountType) {
            newErrors.accountType = "Please choose and account type";
        }

        if(currentStep == 4) {
            if(regData.authMethod == "email") {
                if (!regData.email.trim()) {             
                    newErrors.email = "Please enter an email.";           
                }

                if (!regData.password) {             
                    newErrors.password = "Please enter a password.";           
                }

                if (!regData.confirmPassword) {             
                    newErrors.confirmPassword = "Please confirm your password.";           
                }

                if (regData.password && regData.confirmPassword && 
                    regData.password !== regData.confirmPassword ) {             

                    newErrors.confirmPassword = "Passwords do not match.";           

                }
            }else if(regData.authMethod == "google" && !regData.googleToken){
                newErrors.googleToken = "Google sign in did not complemte, please try again";
            }
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length == 0;
    }


    const goNextStep = () => {     
        const ok = validateInputs();     
        if (!ok) return;     
        if (currentStep == 4) {       
            handleSubmit();       
            return;     
        }     
        setCurrentStep(currentStep + 1);   
    };

    const goPrevStep = () => {     
        if (currentStep > 0) {       
            setCurrentStep(currentStep - 1);       
            setErrors({});     
        }   
    };

     const handleSubmit = async () => {
        //This will have to be written when we finally have an api endpoint for this
        return null
    };

    const renderCurrentSetop = () => {
        console.log(currentStep);
        if(currentStep === 0){
            return(<ChooseAuthMethod setAuthMethod={setAuthMethod} error={errors.authMethod}/>);
        }

        if(currentStep === 1){
            return(<NameSurname name={regData.name} surname={regData.surname} onChange={handleRegDataUpdate} errors={errors}/>);
        }

        if(currentStep === 2){
                return(<DateOfBirth 
                    dateOfBirth={regData.dateOfBirth} 
                    onChange={handleRegDataUpdate} 
                    errors={errors}
                    />
            );
        }

        if(currentStep === 3){
            return(<AccountType 
                accountType={regData.accountType}
                onChange={handleRegDataUpdate}
                errors={errors}
                />
            );
        }

        if(currentStep === 4){
            if(regData.authMethod == "google"){
                return(
                    <FinalizeGoogle googleError={errors.googleToken} onContinueToLogin={()=>navigate("/login")}/>
                    //The lambda expression that is currently the onContinueToLogin will have to also contain the 
                    //post method for the backend data and redirect to login if and only if registering was succesfull
                    //same applies to the emial data bellow
                );
            }

            if(regData.authMethod == "email"){
                return(
                    <EmailPassword email={regData.email} 
                        password={regData.password} 
                        rePassword={regData.rePassword}
                        onChange={handleRegDataUpdate}
                        errors={errors}
                        onRegister={()=>navigate("/login")}/>
                );
            }
        }
    }

const renderNavigationButtons = () => {     
        const isFirst = currentStep === 0;     
        const isLast = currentStep === 4;     

        if(currentStep == 4) return null;

        return (<div className="alternativa" style={{ marginTop: "2rem" }}>         
            {!isFirst && (           
                <button type="button" className="submit-btn" onClick={goPrevStep} style={{ marginRight: "1rem" }}>             
                    Back           
                </button>         
            )}         
            <button type="button" className="submit-btn" onClick={goNextStep}>           
                {isLast ? "Finish" : "Continue"}         
            </button>       
        </div>     
        );   
    };

    return (
        <CloudBackground>
            <WhiteRectangle>
                <p>REGISTRATION</p>
                {renderCurrentSetop()}
                {renderNavigationButtons()}

            </WhiteRectangle>
        </CloudBackground>
    );
}

export default Register;



