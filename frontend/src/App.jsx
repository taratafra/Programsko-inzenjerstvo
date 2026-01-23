import { useState } from "react";
import Login from './pages/Login/Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import Register from "./pages/Register/Register.jsx";
import Home from "./pages/Home/Home.jsx";
import ProtectedRoute from "./utils/ProtectedRoute";
import Questionnaire from "./pages/Questionnaire/Questionnaire.jsx";
import Watch from "./pages/Watch/Watch";
import { ToastProvider } from './components/home/tabPanel/ToastNotification';
import NotificationService from "./components/home/tabPanel/NotificationService.jsx";
import TrainerLobby from "./pages/TrainerLobby/TrainerLobby.jsx";


function App() {
  return (
    <ToastProvider>

      <NotificationService />

      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/questions" element={<Questionnaire />} />
        <Route path="/trainer-lobby" element={<TrainerLobby/>}></Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/home" element={<Home />} />
          <Route path="/watch/:id" element={<Watch />} />
        </Route>
      </Routes>
    </ToastProvider>
  );
}

export default App;