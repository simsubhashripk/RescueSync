import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const from = location.state?.from?.pathname || '/';

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.username.trim() || !formData.password) {
      setError('Please provide both username and password.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await login({
        username: formData.username,
        password: formData.password,
      });
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-badge">Secure Portal Access</div>
          <h2 className="auth-title">Log in to RescueSync</h2>
          <p className="auth-subtitle">
            Enter your credentials to access the disaster coordination terminal.
          </p>
        </div>

        {error && (
          <div className="alert-box alert-error" role="alert">
            <span className="alert-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <div className="input-wrapper">
              <span className="input-icon">👤</span>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                placeholder="e.g. jdoe or admin"
                value={formData.username}
                onChange={handleChange}
                disabled={loading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="input-wrapper">
              <span className="input-icon">🔒</span>
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                required
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                disabled={loading}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                tabIndex="-1"
              >
                {showPassword ? '👁️‍🗨️' : '👁️'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={loading}
          >
            {loading ? (
              <span className="btn-loading-content">
                <span className="mini-spinner"></span>
                <span>Authenticating...</span>
              </span>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        <div className="auth-footer">
          <span>Need an account? </span>
          <Link to="/register" className="auth-link">
            Register as a Public Officer
          </Link>
        </div>
      </div>
    </div>
  );
}

