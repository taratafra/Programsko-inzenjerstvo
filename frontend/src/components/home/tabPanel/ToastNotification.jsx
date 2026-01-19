import React, { useState, useEffect, createContext, useContext } from 'react';
import { X, Bell, CheckCircle, AlertCircle, Info } from 'lucide-react';
import styles from './ToastNotification.module.css';

// Toast Context
const ToastContext = createContext();

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
};

// Toast Provider Component
export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info', duration = 5000) => {
    const id = Date.now() + Math.random();
    const newToast = { id, message, type, duration };
    
    setToasts(prev => [...prev, newToast]);

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }
  };

  const removeToast = (id) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  return (
    <ToastContext.Provider value={{ addToast, removeToast }}>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  );
};

// Toast Container Component
const ToastContainer = ({ toasts, onRemove }) => {
  return (
    <div className={styles.toastContainer}>
      {toasts.map(toast => (
        <Toast
          key={toast.id}
          toast={toast}
          onRemove={() => onRemove(toast.id)}
        />
      ))}
    </div>
  );
};

// Individual Toast Component
const Toast = ({ toast, onRemove }) => {
  const [isExiting, setIsExiting] = useState(false);

  const handleRemove = () => {
    setIsExiting(true);
    setTimeout(onRemove, 300);
  };

  const getIcon = () => {
    const iconProps = { className: styles.toastIcon };
    
    switch (toast.type) {
      case 'success':
        return <CheckCircle {...iconProps} />;
      case 'error':
        return <AlertCircle {...iconProps} />;
      case 'warning':
        return <AlertCircle {...iconProps} />;
      case 'reminder':
        return <Bell {...iconProps} />;
      default:
        return <Info {...iconProps} />;
    }
  };

  return (
    <div className={`${styles.toast} ${styles[toast.type]} ${isExiting ? styles.exiting : ''}`}>
      {getIcon()}
      <div className={styles.toastContent}>
        <p className={styles.toastMessage}>{toast.message}</p>
      </div>
      <button onClick={handleRemove} className={styles.toastCloseButton}>
        <X className={styles.toastCloseIcon} />
      </button>
      {toast.duration > 0 && (
        <div 
          className={styles.toastProgressBar}
          style={{ animationDuration: `${toast.duration}ms` }}
        />
      )}
    </div>
  );
};

export default ToastProvider;