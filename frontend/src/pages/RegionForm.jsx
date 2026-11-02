import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  createRegion,
  updateRegion,
  fetchRegionById,
  estimatePriorityScore,
} from '../services/regionService';

export default function RegionForm() {
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const navigate = useNavigate();
  const { user } = useAuth();

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const [formData, setFormData] = useState({
    regionName: '',
    location: '',
    populationAffected: 100,
    disasterSeverity: 5,
    injuredPeople: 0,
    vulnerablePeople: 0,
    roadAccessibility: 0.8,
    latitude: 13.0827,
    longitude: 80.2707,
  });

  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Live estimated priority score matching desktop formula
  const estimatedScore = estimatePriorityScore(formData);

  useEffect(() => {
    if (isEditMode) {
      setLoading(true);
      fetchRegionById(id)
        .then((data) => {
          setFormData({
            regionName: data.regionName || '',
            location: data.location || '',
            populationAffected: data.populationAffected ?? 0,
            disasterSeverity: data.disasterSeverity ?? 1,
            injuredPeople: data.injuredPeople ?? 0,
            vulnerablePeople: data.vulnerablePeople ?? 0,
            roadAccessibility: data.roadAccessibility ?? 1.0,
            latitude: data.latitude ?? 0.0,
            longitude: data.longitude ?? 0.0,
          });
        })
        .catch((err) => {
          setError(err.message || 'Failed to load region for editing.');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [id, isEditMode]);

  // Authorization Guard: Only Admins can register/edit
  if (!isAdmin) {
    return (
      <div className="main-content">
        <div className="alert-box alert-error" style={{ margin: '2rem 0' }}>
          <span className="alert-icon">🚫</span>
          <div>
            <strong>Administrative Clearance Required</strong>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.9rem' }}>
              Only system administrators have permission to register or calibrate disaster-affected sectors.
            </p>
          </div>
        </div>
        <Link to="/regions" className="btn btn-outline-dark">
          ← Return to Disaster Areas Directory
        </Link>
      </div>
    );
  }

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    let parsedValue = value;
    if (type === 'number' || type === 'range') {
      parsedValue = value === '' ? '' : Number(value);
    }
    setFormData((prev) => ({
      ...prev,
      [name]: parsedValue,
    }));
    if (error) setError('');
  };

  const validate = () => {
    if (!formData.regionName.trim()) {
      return 'Region name is required.';
    }
    if (!formData.location.trim()) {
      return 'Location description is required.';
    }
    if (formData.populationAffected < 0) {
      return 'Population affected must be non-negative.';
    }
    if (formData.disasterSeverity < 1 || formData.disasterSeverity > 10) {
      return 'Disaster severity must be between 1 and 10.';
    }
    if (formData.injuredPeople < 0) {
      return 'Injured people count must be non-negative.';
    }
    if (formData.vulnerablePeople < 0) {
      return 'Vulnerable people count must be non-negative.';
    }
    if (
      formData.populationAffected > 0 &&
      formData.injuredPeople > formData.populationAffected
    ) {
      return `Injured count (${formData.injuredPeople}) cannot exceed total population affected (${formData.populationAffected}).`;
    }
    if (formData.roadAccessibility < 0.0 || formData.roadAccessibility > 1.0) {
      return 'Road accessibility must be between 0.0 and 1.0.';
    }
    if (formData.latitude < -90.0 || formData.latitude > 90.0) {
      return 'Latitude must be between -90.0 and 90.0 degrees.';
    }
    if (formData.longitude < -180.0 || formData.longitude > 180.0) {
      return 'Longitude must be between -180.0 and 180.0 degrees.';
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

    setSubmitting(true);
    setError('');
    setSuccessMsg('');

    const payload = {
      regionName: formData.regionName.trim(),
      location: formData.location.trim(),
      populationAffected: Number(formData.populationAffected),
      disasterSeverity: Number(formData.disasterSeverity),
      injuredPeople: Number(formData.injuredPeople),
      vulnerablePeople: Number(formData.vulnerablePeople),
      roadAccessibility: Number(formData.roadAccessibility),
      latitude: Number(formData.latitude),
      longitude: Number(formData.longitude),
    };

    try {
      if (isEditMode) {
        await updateRegion(id, payload);
        setSuccessMsg(`Region "${payload.regionName}" updated successfully.`);
      } else {
        await createRegion(payload);
        setSuccessMsg(`Region "${payload.regionName}" registered successfully.`);
      }
      setTimeout(() => {
        navigate('/regions');
      }, 1200);
    } catch (err) {
      setError(err.message || 'Operation failed.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="main-content">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading region configuration...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/regions">Disaster Affected Regions</Link> /{' '}
            <span>{isEditMode ? `Edit Sector #${id}` : 'Register New Sector'}</span>
          </div>
          <h1 className="page-title">
            {isEditMode ? `Edit Disaster Area: ${formData.regionName}` : 'Register Disaster-Affected Sector'}
          </h1>
          <p className="page-desc">
            Define incident metrics, geographic coordinates, and logistics parameters to calculate automated triage scores.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert-box alert-error" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="alert-box alert-success" role="alert">
          <span className="alert-icon">✅</span>
          <span>{successMsg} Redirecting to region directory...</span>
        </div>
      )}

      <div className="form-layout-grid">
        <div className="form-card">
          <form onSubmit={handleSubmit} noValidate>
            <h3 className="section-title">Sector Identity & Demographics</h3>

            <div className="form-group">
              <label htmlFor="regionName">Region Name *</label>
              <input
                id="regionName"
                name="regionName"
                type="text"
                required
                placeholder="e.g. Sector E (River Basin)"
                value={formData.regionName}
                onChange={handleChange}
                disabled={submitting}
              />
              <span className="field-hint">Unique official designation for the disaster zone.</span>
            </div>

            <div className="form-group">
              <label htmlFor="location">Location / Landmark *</label>
              <input
                id="location"
                name="location"
                type="text"
                required
                placeholder="e.g. Northern Flood Basin District"
                value={formData.location}
                onChange={handleChange}
                disabled={submitting}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="populationAffected">Population Affected *</label>
                <input
                  id="populationAffected"
                  name="populationAffected"
                  type="number"
                  min="0"
                  required
                  value={formData.populationAffected}
                  onChange={handleChange}
                  disabled={submitting}
                />
              </div>

              <div className="form-group">
                <label htmlFor="disasterSeverity">
                  Disaster Severity (1 - 10): <strong>Level {formData.disasterSeverity}</strong>
                </label>
                <div className="slider-wrapper">
                  <input
                    id="disasterSeverity"
                    name="disasterSeverity"
                    type="range"
                    min="1"
                    max="10"
                    step="1"
                    value={formData.disasterSeverity}
                    onChange={handleChange}
                    disabled={submitting}
                  />
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={formData.disasterSeverity}
                    onChange={handleChange}
                    style={{ width: '70px', textAlign: 'center' }}
                    disabled={submitting}
                  />
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="injuredPeople">Injured Individuals *</label>
                <input
                  id="injuredPeople"
                  name="injuredPeople"
                  type="number"
                  min="0"
                  required
                  value={formData.injuredPeople}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <span className="field-hint">Persons requiring medical assistance.</span>
              </div>

              <div className="form-group">
                <label htmlFor="vulnerablePeople">Vulnerable Population *</label>
                <input
                  id="vulnerablePeople"
                  name="vulnerablePeople"
                  type="number"
                  min="0"
                  required
                  value={formData.vulnerablePeople}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <span className="field-hint">Children, elderly, disabled individuals.</span>
              </div>
            </div>

            <h3 className="section-title" style={{ marginTop: '2rem' }}>
              Logistics & Coordinates
            </h3>

            <div className="form-group">
              <label htmlFor="roadAccessibility">
                Road Accessibility: <strong>{Math.round(formData.roadAccessibility * 100)}% Open</strong>
              </label>
              <div className="slider-wrapper">
                <input
                  id="roadAccessibility"
                  name="roadAccessibility"
                  type="range"
                  min="0.0"
                  max="1.0"
                  step="0.05"
                  value={formData.roadAccessibility}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <input
                  type="number"
                  min="0.0"
                  max="1.0"
                  step="0.01"
                  value={formData.roadAccessibility}
                  onChange={handleChange}
                  style={{ width: '75px', textAlign: 'center' }}
                  disabled={submitting}
                />
              </div>
              <span className="field-hint">
                0.0 = completely impassable routes • 1.0 = clear highway corridor.
              </span>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="latitude">Latitude (-90.0 to 90.0) *</label>
                <input
                  id="latitude"
                  name="latitude"
                  type="number"
                  step="0.0001"
                  min="-90"
                  max="90"
                  required
                  value={formData.latitude}
                  onChange={handleChange}
                  disabled={submitting}
                />
              </div>

              <div className="form-group">
                <label htmlFor="longitude">Longitude (-180.0 to 180.0) *</label>
                <input
                  id="longitude"
                  name="longitude"
                  type="number"
                  step="0.0001"
                  min="-180"
                  max="180"
                  required
                  value={formData.longitude}
                  onChange={handleChange}
                  disabled={submitting}
                />
              </div>
            </div>

            <div className="form-actions" style={{ marginTop: '2rem' }}>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting}
              >
                {submitting ? (
                  <span className="btn-loading-content">
                    <span className="mini-spinner"></span>
                    <span>Saving Sector...</span>
                  </span>
                ) : isEditMode ? (
                  'Update Disaster Area'
                ) : (
                  'Register Disaster Area'
                )}
              </button>
              <button
                type="button"
                className="btn btn-outline-dark"
                onClick={() => navigate('/regions')}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>

        <div className="preview-card">
          <div className="preview-header">
            <h4>Live Priority Score Estimator</h4>
            <span className="preview-badge">Deterministic Algorithm</span>
          </div>

          <div className="score-display-box">
            <div className="large-score-number">{estimatedScore.toFixed(2)}</div>
            <div className="score-scale">out of 100.00 max urgency</div>
          </div>

          <div className="formula-breakdown">
            <h5>Weight Distribution Breakdown:</h5>
            <ul className="breakdown-list">
              <li>
                <span>Severity (35% max):</span>
                <strong>{((formData.disasterSeverity / 10.0) * 35.0).toFixed(2)} pts</strong>
              </li>
              <li>
                <span>Injury Ratio (25% max):</span>
                <strong>
                  {(
                    (formData.populationAffected > 0
                      ? Math.min(1.0, formData.injuredPeople / formData.populationAffected)
                      : 0.0) * 25.0
                  ).toFixed(2)}{' '}
                  pts
                </strong>
              </li>
              <li>
                <span>Vulnerable Ratio (20% max):</span>
                <strong>
                  {(
                    (formData.populationAffected > 0
                      ? Math.min(1.0, formData.vulnerablePeople / formData.populationAffected)
                      : 0.0) * 20.0
                  ).toFixed(2)}{' '}
                  pts
                </strong>
              </li>
              <li>
                <span>Population Scale (10% max):</span>
                <strong>
                  {(
                    Math.min(
                      1.0,
                      Math.log10(Math.max(10, formData.populationAffected || 0)) / 4.0
                    ) * 10.0
                  ).toFixed(2)}{' '}
                  pts
                </strong>
              </li>
              <li>
                <span>Road Inaccessibility (10% max):</span>
                <strong>
                  {((1.0 - (formData.roadAccessibility || 0.0)) * 10.0).toFixed(2)} pts
                </strong>
              </li>
            </ul>
          </div>

          <div className="formula-note">
            <span style={{ fontSize: '1.1rem' }}>ℹ️</span>
            <span>
              The algorithm prioritizes areas with high casualties, vulnerable demographics, and blocked roads to deploy aid where survival windows are shortest.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

