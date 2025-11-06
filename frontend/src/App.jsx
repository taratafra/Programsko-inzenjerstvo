import { useState } from "react";
import './App.css';
import Login from './Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import Register from "./Register";
import Home from "./Home";
import ProtectedRoute from "./ProtectedRoute";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/home" element={<Home />} />
      
      <Route element={<ProtectedRoute />}>

        {/* !!!!!!!!!!!!! treba popravit ProtectedRoutes.js inace necete moc nigdi ili ako vam to ne uspije javite se meni pa cu
        privremeno dodat rutu dok se ne popravi ovo*/}

        {/* tu dodajite sve rute od sad pa nadalje al aj neka neko provjeri jel radi ovo*/}

      </Route>
    </Routes>
  );
}

export default App;
