import {useState} from "react";
import './App.css';
import Login from './Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import Register from "./Register";

function App() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/login" replace/>}/>
            <Route path="/login" element={<Login />}/>
            <Route path="/register" element={<Register />}/>
        </Routes>

    
    );
}

export default App;



