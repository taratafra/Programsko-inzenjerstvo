import { useState } from "react";
import './App.css';


function App() {
    const [message, setMessage] = useState("");
    const BACKEND_URL = process.env.REACT_APP_BACKEND
  const handleClick = async () => {
    try {
      const response = await fetch(BACKEND_URL);
      const text = await response.text();
      setMessage(text);
    } catch (error) {
      console.error("Error:", error);
      setMessage("Failed to connect to backend");
    }
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>React ↔ Spring Boot Connection</h1>
        <button onClick={handleClick}>Connect to Backend</button>
        <p>{message}</p>
      </header>
    </div>
  );
}

export default App;

