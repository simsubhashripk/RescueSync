import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <NavLink to="/" className="brand-container">
        <span className="brand-logo" role="img" aria-label="shield">🛡️</span>
        <div>
          <div className="brand-title">RESCUESYNC</div>
          <div className="brand-subtitle">Smart Disaster Resource Allocation System</div>
        </div>
      </NavLink>

      <div className="nav-actions">
        <nav className="nav-links">
          <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            Home
          </NavLink>
          {isAuthenticated && (
            <>
              <NavLink to="/regions" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Disaster Areas
              </NavLink>
              <NavLink to="/resources" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Resource Inventory
              </NavLink>
              <NavLink to="/requests" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Relief Demands
              </NavLink>
              <NavLink to="/requests/queue" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Priority Queue
              </NavLink>
              <NavLink to="/allocations" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Allocations
              </NavLink>
              <NavLink to="/map" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Transit Map
              </NavLink>
              <NavLink to="/redistribution" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Redistribution
              </NavLink>
              <NavLink to="/analytics" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Analytics &amp; Reports
              </NavLink>
            </>
          )}
        </nav>

        {isAuthenticated && user ? (
          <div className="user-profile-badge">
            <div className="user-info">
              <span className="user-name">{user.fullName || user.username}</span>
              <span className={`user-role-tag role-${user.role?.toLowerCase()}`}>
                {user.role}
              </span>
            </div>
            <button
              onClick={handleLogout}
              className="btn btn-outline-danger btn-sm"
              title="Sign out of RescueSync"
            >
              Sign Out
            </button>
          </div>
        ) : (
          <div className="auth-buttons">
            <NavLink to="/login" className="btn btn-outline-light btn-sm">
              Sign In
            </NavLink>
            <NavLink to="/register" className="btn btn-primary btn-sm">
              Register
            </NavLink>
          </div>
        )}
      </div>
    </header>
  );
}
