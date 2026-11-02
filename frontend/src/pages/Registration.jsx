import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Registration() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
    if (error) setError('');
  };

  const validate = () => {
    if (!formData.fullName.trim()) {
      return 'Full name is required.';
    }
    if (!formData.username.trim()) {
      return 'Username is required.';
    }
    if (formData.username.trim().length < 3) {
      return 'Username must be at least 3 characters.';
    }
    if (!formData.email.trim()) {
      return 'Email address is required.';
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email.trim())) {
      return 'Please provide a valid email address.';
    }
    if (!formData.password) {
      return 'Password is required.';
    }
    if (formData.password.length < 6) {
      return 'Password must be at least 6 characters.';
    }
    if (formData.password !== formData.confirmPassword) {
      return 'Passwords do not match.';
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError('');

    try {
      await register({
        fullName: formData.fullName,
        username: formData.username,
        email: formData.email,
        password: formData.password,
      });
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'Registration failed. Please verify your details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-badge">Officer Onboarding</div>
          <h2 className="auth-title">Register for RescueSync</h2>
          <p className="auth-subtitle">
            Create an official public responder account to coordinate disaster relief.
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
            <label htmlFor="fullName">Full Name</label>
            <div className="input-wrapper">
              <span className="input-icon">📝</span>
              <input
                id="fullName"
                name="fullName"
                type="text"
                autoComplete="name"
                required
                placeholder="e.g. Inspector John Doe"
                value={formData.fullName}
                onChange={handleChange}
                disabled={loading}
              />
            </div>
          </div>

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
                placeholder="e.g. jdoe_rescue"
                value={formData.username}
                onChange={handleChange}
                disabled={loading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <div className="input-wrapper">
              <span className="input-icon">✉️</span>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                placeholder="e.g. jdoe@rescuesync.gov"
                value={formData.email}
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
                autoComplete="new-password"
                required
                placeholder="At least 6 characters"
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

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <div className="input-wrapper">
              <span className="input-icon">🔒</span>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                autoComplete="new-password"
                required
                placeholder="Re-enter your password"
                value={formData.confirmPassword}
                onChange={handleChange}
                disabled={loading}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                tabIndex="-1"
              >
                {showConfirmPassword ? '👁️‍🗨️' : '👁️'}
              </button>
            </div>
          </div>

          <div className="role-notice">
            <span className="role-notice-icon">ℹ️</span>
            <span>Account Role: <strong>PUBLIC OFFICER (USER)</strong>. Administrative clearance is restricted.</span>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={loading}
          >
            {loading ? (
              <span className="btn-loading-content">
                <span className="mini-spinner"></span>
                <span>Creating Account...</span>
              </span>
            ) : (
              'Complete Registration'
            )}
          </button>
        </form>

        <div className="auth-footer">
          <span>Already registered? </span>
          <Link to="/login" className="auth-link">
            Log in here
          </Link>
        </div>
      </div>
    </div>
  );
}

