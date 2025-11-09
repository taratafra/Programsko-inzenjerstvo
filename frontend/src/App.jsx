import { useState } from "react";
import './App.css';
import Login from './Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import Register from "./Register";
import Home from "./Home";
import ProtectedRoute from "./ProtectedRoute";
import Questionnaire from "./Questionnaire";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/questions" element={<Questionnaire />} />
            
      <Route element={<ProtectedRoute />}>
        <Route path="/home" element={<Home />} />
      </Route>
    </Routes>
  );
}

export default App;
