import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  getStoredToken,
  getStoredUser,
  setSession,
  clearSession,
  loginUser,
  registerUser,
  fetchCurrentUser,
} from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getStoredToken());
  const [user, setUser] = useState(() => getStoredUser());
  const [loading, setLoading] = useState(true);

  // Validate session on app initialization
  useEffect(() => {
    async function verifyAuth() {
      const existingToken = getStoredToken();
      if (!existingToken) {
        setUser(null);
        setLoading(false);
        return;
      }

      try {
        const verifiedUser = await fetchCurrentUser();
        if (verifiedUser) {
          setUser(verifiedUser);
          setToken(existingToken);
        } else {
          clearSession();
          setUser(null);
          setToken(null);
        }
      } catch (err) {
        console.error('Session validation error:', err);
        clearSession();
        setUser(null);
        setToken(null);
      } finally {
        setLoading(false);
      }
    }

    verifyAuth();
  }, []);

  const login = async (credentials) => {
    const data = await loginUser(credentials);
    setToken(data.token);
    setUser(data.user);
    return data.user;
  };

  const register = async (formData) => {
    const data = await registerUser(formData);
    setToken(data.token);
    setUser(data.user);
    return data.user;
  };

  const logout = () => {
    clearSession();
    setToken(null);
    setUser(null);
  };

  const refreshUser = async () => {
    const verifiedUser = await fetchCurrentUser();
    setUser(verifiedUser);
    return verifiedUser;
  };

  const value = {
    user,
    token,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    register,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

