import { useState } from "react";
import './styles/App.css';
import Login from './pages/Login/Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import Register from "./pages/Register/Register";
import Home from "./pages/Home/Home";
import ProtectedRoute from "./utils/ProtectedRoute";
import Questionnaire from "./pages/Questionnaire/Questionnaire";

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
