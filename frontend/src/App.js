import { useState } from "react";
import './App.css';


function App() {
  const [message, setMessage] = useState("");

  const handleClick = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/connect");
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

