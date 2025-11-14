import { useState } from "react";
import "./Register.css";
import CloudBackground from "./components/backgrounds/CloudyBackground";
import WhiteRectangle from "./components/backgrounds/WhiteRectangle";
import { useNavigate } from "react-router-dom";

function Register() {
  const navigate = useNavigate();
  const BACKEND_URL = process.env.REACT_APP_BACKEND; 

  const [regData, setRegData] = useState({
    email: "",
    password: "",
    name: "",
    surname: "",
    dateOfBirth: "",
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const handleRegDataUpdate = (field, value) => {
    setRegData((prev) => ({ ...prev, [field]: value }));
  };
    const validatePasswordStrength = (password) => {
    const errors = [];
    
    if (password.length < 8) {
      errors.push("At least 8 characters");
    }
    if (!/[A-Z]/.test(password)) {
      errors.push("At least one uppercase letter");
    }
    if (!/[a-z]/.test(password)) {
      errors.push("At least one lowercase letter");
    }
    if (!/[0-9]/.test(password)) {
      errors.push("At least one number");
    }
    if (!/[!@#$%^&*(),.?":{}|<>\-\+]/.test(password)) {
      errors.push("At least one special character");
    }
    
    return errors;
  };

  const validateInputs = () => {
    const newErrors = {};
    if (!regData.email.trim()) newErrors.email = "Please enter an email.";
    if (!regData.password.trim()) newErrors.password = "Please enter a password.";
    if (regData.password.length < 6)
      newErrors.password = "Password must be at least 6 characters.";
    if (!regData.name.trim()) newErrors.name = "Please enter your name.";
    if (!regData.surname.trim()) newErrors.surname = "Please enter your surname.";
    if (!regData.dateOfBirth.trim())
      newErrors.dateOfBirth = "Please enter your date of birth.";

    if (!regData.password.trim()) {
      newErrors.password = "Please enter a password.";
    } else {
      const passwordErrors = validatePasswordStrength(regData.password);
      if (passwordErrors.length > 0) {
        newErrors.password = passwordErrors.join(", ");
      }
    }
    if (regData.password.length < 6)
      newErrors.password = "Password must be at least 6 characters.";
    if (!regData.name.trim()) newErrors.name = "Please enter your name.";
    if (!regData.surname.trim()) newErrors.surname = "Please enter your surname.";
    if (!regData.dateOfBirth.trim())
      newErrors.dateOfBirth = "Please enter your date of birth.";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateInputs()) return;

    setLoading(true);
    setMessage("");
    try {
      const response = await fetch(`${BACKEND_URL}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(regData),
      });

      const text = await response.text();

      if (!response.ok) {
        throw new Error(text || "Registration failed");
      }

      setMessage("Registration successful! You can now log in.");
      setTimeout(() => navigate("/login"), 1200);
    } catch (error) {
      console.error("Registration error:", error);
      setMessage(error.message || "Failed to register");
    } finally {
      setLoading(false);
    }
  };

  return (
    <CloudBackground>
      <WhiteRectangle>
        <p className="LOGIN">REGISTER</p>
        <form className="popuni" onSubmit={handleSubmit}>
          <div className="upisi">  
            <input
              type="email"
              placeholder="Email"
              value={regData.email}
              onChange={(e) => handleRegDataUpdate("email", e.target.value)}
              required
            />
            {errors.email && <p className="error">{errors.email}</p>}

            <input
              type="password"
              placeholder="Password"
              value={regData.password}
              onChange={(e) => handleRegDataUpdate("password", e.target.value)}
              required
            />
            {errors.password && <p className="error">{errors.password}</p>}

            <input
              type="text"
              placeholder="Name"
              value={regData.name}
              onChange={(e) => handleRegDataUpdate("name", e.target.value)}
              required
            />
            {errors.name && <p className="error">{errors.name}</p>}

            <input
              type="text"
              placeholder="Surname"
              value={regData.surname}
              onChange={(e) => handleRegDataUpdate("surname", e.target.value)}
              required
            />
            {errors.surname && <p className="error">{errors.surname}</p>}

            <input
              type="date"
              value={regData.dateOfBirth}
              onChange={(e) => handleRegDataUpdate("dateOfBirth", e.target.value)}
              required
            />
            {errors.dateOfBirth && <p className="error">{errors.dateOfBirth}</p>}
          </div>
          <button type="submit" className="reg-btn" disabled={loading}>
            {loading ? "Registering..." : "Register"}
          </button>
          
          {message && <p className="info-message">{message}</p>}
        </form>
      </WhiteRectangle>
    </CloudBackground>
  );
}

export default Register;
