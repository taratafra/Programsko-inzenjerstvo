import { useState } from "react";
import './Register.css';
import CloudBackground from "./components/backgrounds/CloudyBackground";
import WhiteRectangle from "./components/backgrounds/WhiteRectangle"
import EmailPassword from "./components/registerCards/EmailPassword";
import { useNavigate } from "react-router-dom";

function Register() {
    const [currentStep, setCurrentStep]=useState(0);
    
    const navigate = useNavigate();

    const [regData, setRegData] = useState({
        email: "",
        password: "",
    });

    const [errors, setErrors] = useState({});

    const handleRegDataUpdate = (field, value) => {
        setRegData((prev)=> ({
            ...prev,
            [field]: value,
            }));
    };

    const validateInputs = () => {
        const newErrors = {}

        if(currentStep == 0) {
            if (!regData.email.trim()) {             
                newErrors.email = "Please enter an email.";           
            }
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length == 0;
    }


     const handleSubmit = async () => {
        //This will have to be written when we finally have an api endpoint for this
        return null
    };

    const renderCurrentSetop = () => {
        console.log(currentStep);
 
        if(currentStep === 0){
            return(
                <EmailPassword email={regData.email} 
                    password={regData.password} 
                    onChange={handleRegDataUpdate}
                    errors={errors}
                    onRegister={()=>{
                        if(validateInputs()){
                            navigate("/login");
                        }
                    }}
                />
            );
        }
    }

    return (
        <CloudBackground>
            <WhiteRectangle>
                <p className="LOGIN">REGISTRATION</p>
                {renderCurrentSetop()}

            </WhiteRectangle>
        </CloudBackground>
    );
}

export default Register;



