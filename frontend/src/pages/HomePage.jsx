import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { checkBackendHealth } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function HomePage() {
  const { user, isAuthenticated } = useAuth();
  const [health, setHealth] = useState({ status: 'CHECKING', application: '' });

  useEffect(() => {
    checkBackendHealth().then((data) => {
      setHealth(data);
    });
  }, []);

  return (
    <div className="main-content">
      <section className="hero-card">
        <span className="hero-badge">Emergency Response Operations Active</span>
        <h1 className="hero-title">Disaster Response Operations</h1>
        <p className="hero-desc">
          A centralized platform for coordinating emergency resources, monitoring relief demands, and managing disaster response activities.
        </p>

        {isAuthenticated && user ? (
          <div className="user-welcome-banner">
            <div className="welcome-avatar">🛡️</div>
            <div className="welcome-details">
              <h3>Welcome back, {user.fullName || user.username}!</h3>
              <p>
                Signed in as <strong>{user.username}</strong> ({user.email}) • Account: <span className="role-tag">{user.role}</span>
              </p>
              <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <Link to="/analytics" className="btn btn-primary btn-sm">
                  📊 Analytics &amp; Reports
                </Link>
                <Link to="/redistribution" className="btn btn-outline-dark btn-sm">
                  🔄 Resource Redistribution
                </Link>
                <Link to="/map" className="btn btn-outline-dark btn-sm">
                  🗺️ Transit Route Map
                </Link>
                <Link to="/allocations" className="btn btn-outline-dark btn-sm">
                  📑 Resource Allocations
                </Link>
                <Link to="/requests/queue" className="btn btn-outline-dark btn-sm">
                  ⚡ Priority Queue
                </Link>
                <Link to="/requests" className="btn btn-outline-dark btn-sm">
                  📋 Relief Demands
                </Link>
                <Link to="/resources" className="btn btn-outline-dark btn-sm">
                  📦 Inventory
                </Link>
                <Link to="/regions" className="btn btn-outline-dark btn-sm">
                  📍 Disaster Areas
                </Link>
              </div>
            </div>
          </div>
        ) : (
          <div className="auth-callout-banner">
            <div>
              <strong>Officer Access Required:</strong>
              <p style={{ margin: '0.25rem 0 0', color: '#64748b', fontSize: '0.9rem' }}>
                Sign in with your verified credentials or register as a public responder.
              </p>
            </div>
            <div className="callout-actions">
              <Link to="/login" className="btn btn-primary btn-sm">
                Sign In
              </Link>
              <Link to="/register" className="btn btn-outline-dark btn-sm">
                Register
              </Link>
            </div>
          </div>
        )}

        <div className="backend-status-card">
          <div>
            <strong>System Status:</strong>
            <p style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.2rem' }}>
              Operational Platform Network
            </p>
          </div>
          <div className="status-indicator">
            <span
              className={`status-dot ${
                health.status === 'UP' ? 'up' : health.status === 'DOWN' ? 'down' : ''
              }`}
            />
            <span>
              {health.status === 'UP'
                ? 'Online'
                : health.status === 'CHECKING'
                ? 'Connecting...'
                : 'Offline'}
            </span>
          </div>
        </div>
      </section>

      <section className="features-grid">
        <div className="feature-box">
          <h3><span>📊</span> Emergency Demand Monitoring</h3>
          <p>
            Continuous tracking and priority assessment of emergency demands based on disaster severity,
            affected populations, and regional vulnerability.
          </p>
        </div>
        <div className="feature-box">
          <h3><span>🛣️</span> Optimized Relief Routes</h3>
          <p>
            Logistics calculation identifying clear routes and transit times between supply depots and
            affected areas.
          </p>
        </div>
        <div className="feature-box">
          <h3><span>🔄</span> Resource Redistribution</h3>
          <p>
            Automated balance monitoring identifying surplus supplies to meet critical deficits across regions.
          </p>
        </div>
      </section>
    </div>
  );
}
