import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchRegionById, deleteRegion } from '../services/regionService';

export default function RegionDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [region, setRegion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState(false);

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  useEffect(() => {
    setLoading(true);
    fetchRegionById(id)
      .then((data) => {
        setRegion(data);
      })
      .catch((err) => {
        setError(err.message || `Failed to load sector #${id}`);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm(`Are you sure you want to delete ${region.regionName}? Associated relief operations and records may prevent deletion.`)) {
      return;
    }
    setDeleting(true);
    setError('');
    try {
      await deleteRegion(id);
      navigate('/regions');
    } catch (err) {
      setError(err.message);
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="main-content">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Retrieving disaster sector details...</p>
        </div>
      </div>
    );
  }

  if (error && !region) {
    return (
      <div className="main-content">
        <div className="alert-box alert-error">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
        <Link to="/regions" className="btn btn-outline-dark">
          ← Back to Region Directory
        </Link>
      </div>
    );
  }

  const injuryPercent = region.populationAffected > 0
    ? ((region.injuredPeople / region.populationAffected) * 100).toFixed(1)
    : '0.0';

  const vulnerablePercent = region.populationAffected > 0
    ? ((region.vulnerablePeople / region.populationAffected) * 100).toFixed(1)
    : '0.0';

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/regions">Disaster Affected Regions</Link> / <span>Sector #{region.regionId}</span>
          </div>
          <h1 className="page-title">{region.regionName}</h1>
          <p className="page-desc">
            Geographic coordinates: <code>{region.latitude.toFixed(4)}° N, {region.longitude.toFixed(4)}° E</code> • {region.location}
          </p>
        </div>
        <div className="detail-actions">
          <Link to="/regions" className="btn btn-outline-dark">
            ← Back to Directory
          </Link>
          {isAdmin && (
            <>
              <Link to={`/regions/${region.regionId}/edit`} className="btn btn-primary">
                Edit Sector
              </Link>
              <button
                onClick={handleDelete}
                className="btn btn-outline-danger"
                disabled={deleting}
              >
                {deleting ? 'Checking...' : 'Delete'}
              </button>
            </>
          )}
        </div>
      </div>

      {error && (
        <div className="alert-box alert-error" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* Main Metric Spotlight */}
      <div className="metric-spotlight-grid">
        <div className="spotlight-card priority-spotlight">
          <span className="spotlight-label">Disaster Priority Triage Score</span>
          <div className="spotlight-value">
            {region.priorityScore?.toFixed(2)}
            <span className="spotlight-sub"> / 100</span>
          </div>
          <p className="spotlight-caption">
            Computed dynamically based on severity rating, casualty urgency, and route accessibility deficits.
          </p>
        </div>

        <div className="spotlight-card">
          <span className="spotlight-label">Disaster Severity Rating</span>
          <div className="spotlight-value">
            Level {region.disasterSeverity}
            <span className="spotlight-sub"> / 10</span>
          </div>
          <div className="severity-bar-track">
            <div
              className="severity-bar-fill"
              style={{ width: `${(region.disasterSeverity / 10) * 100}%` }}
            />
          </div>
          <p className="spotlight-caption">
            {region.disasterSeverity >= 8
              ? 'Catastrophic disaster zone requiring maximum response assets.'
              : region.disasterSeverity >= 5
              ? 'Severe operational conditions with ongoing hazard.'
              : 'Moderate disturbance requiring focused logistical relief.'}
          </p>
        </div>

        <div className="spotlight-card">
          <span className="spotlight-label">Road Transit Accessibility</span>
          <div className="spotlight-value">
            {Math.round(region.roadAccessibility * 100)}%
            <span className="spotlight-sub"> corridor capacity</span>
          </div>
          <div className="severity-bar-track">
            <div
              className="severity-bar-fill road-fill"
              style={{ width: `${region.roadAccessibility * 100}%` }}
            />
          </div>
          <p className="spotlight-caption">
            {region.roadAccessibility <= 0.3
              ? 'High transit blockage! Ground logistics restricted; air/offroad corridors needed.'
              : region.roadAccessibility <= 0.7
              ? 'Partial obstruction; slower transit speeds and alternate routing required.'
              : 'Normal transit corridor open for heavy aid convoy movement.'}
          </p>
        </div>
      </div>

      {/* Demographic Breakdown Cards */}
      <div className="demographics-grid">
        <div className="detail-box">
          <h3>Demographic Impact Summary</h3>
          <table className="info-table">
            <tbody>
              <tr>
                <td>Total Population Impacted:</td>
                <td><strong>{region.populationAffected?.toLocaleString()} persons</strong></td>
              </tr>
              <tr>
                <td>Casualties / Injured:</td>
                <td>
                  <strong style={{ color: '#dc2626' }}>
                    {region.injuredPeople?.toLocaleString()} ({injuryPercent}%)
                  </strong>
                </td>
              </tr>
              <tr>
                <td>Vulnerable Population:</td>
                <td>
                  <strong style={{ color: '#d97706' }}>
                    {region.vulnerablePeople?.toLocaleString()} ({vulnerablePercent}%)
                  </strong>
                </td>
              </tr>
              <tr>
                <td>Registration Timestamp:</td>
                <td>{region.createdAt ? new Date(region.createdAt).toLocaleString() : 'System Baseline'}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="detail-box">
          <h3>Geographic & Logistical Coordinates</h3>
          <table className="info-table">
            <tbody>
              <tr>
                <td>Primary Landmark:</td>
                <td><strong>{region.location}</strong></td>
              </tr>
              <tr>
                <td>Latitude:</td>
                <td><code>{region.latitude?.toFixed(6)}</code></td>
              </tr>
              <tr>
                <td>Longitude:</td>
                <td><code>{region.longitude?.toFixed(6)}</code></td>
              </tr>
              <tr>
                <td>Relational Status:</td>
                <td><span className="badge badge-active">Protected Entity</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

