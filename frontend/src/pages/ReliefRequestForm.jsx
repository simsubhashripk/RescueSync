import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { fetchAllRegions } from '../services/regionService';
import { createReliefRequest } from '../services/requestService';

const PRESET_RESOURCES = [
  'Water',
  'Food',
  'Medicines',
  'Ambulances',
  'Rescue equipment',
  'Tents',
  'Blankets',
  'Fuel',
  'Generators',
];

export default function ReliefRequestForm() {
  const navigate = useNavigate();

  const [regions, setRegions] = useState([]);
  const [formData, setFormData] = useState({
    regionId: '',
    resourceRequired: '',
    requiredQuantity: 100,
  });

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successResult, setSuccessResult] = useState(null);

  useEffect(() => {
    async function loadRegions() {
      setLoading(true);
      setError('');
      try {
        const regionList = await fetchAllRegions();
        setRegions(regionList);
        if (regionList.length > 0) {
          setFormData((prev) => ({
            ...prev,
            regionId: regionList[0].regionId,
          }));
        }
      } catch (err) {
        setError(err.message || 'Failed to retrieve disaster regions list.');
      } finally {
        setLoading(false);
      }
    }

    loadRegions();
  }, []);

  const selectedRegion = regions.find((r) => String(r.regionId) === String(formData.regionId));

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    let parsedValue = value;
    if (type === 'number') {
      parsedValue = value === '' ? '' : parseInt(value, 10);
    }
    setFormData((prev) => ({
      ...prev,
      [name]: parsedValue,
    }));
    if (error) setError('');
  };

  const handleSelectPreset = (name) => {
    setFormData((prev) => ({
      ...prev,
      resourceRequired: name,
    }));
    if (error) setError('');
  };

  const validate = () => {
    if (!formData.regionId) {
      return 'Please choose a target disaster sector.';
    }
    if (!formData.resourceRequired.trim()) {
      return 'Resource category / name is required.';
    }
    if (!formData.requiredQuantity || formData.requiredQuantity <= 0) {
      return 'Required quantity must be strictly greater than zero.';
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
    setSuccessResult(null);

    const payload = {
      regionId: Number(formData.regionId),
      resourceRequired: formData.resourceRequired.trim(),
      requiredQuantity: Number(formData.requiredQuantity),
    };

    try {
      const response = await createReliefRequest(payload);
      setSuccessResult(response);
      setTimeout(() => {
        navigate('/requests');
      }, 1600);
    } catch (err) {
      setError(err.message || 'Failed to submit relief request.');
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="main-content">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading registered disaster regions...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Dashboard</Link> / <Link to="/requests">Relief Requests</Link> / <span>Submit Demand</span>
          </div>
          <h1 className="page-title">Submit Emergency Relief Demand</h1>
          <p className="page-desc">
            Register a vital relief resource demand for an affected disaster sector. The request will automatically receive the sector's emergency priority rating.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert-box alert-error" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {successResult && (
        <div className="alert-box alert-success" role="alert">
          <span className="alert-icon">✅</span>
          <div>
            <strong>Relief Request #{successResult.requestId} Created Successfully!</strong>
            <p style={{ margin: '0.2rem 0 0', fontSize: '0.9rem' }}>
              Resource: <strong>{successResult.resourceRequired}</strong> ({successResult.requiredQuantity?.toLocaleString()} units) • Inherited Priority Score: <strong>★ {successResult.priorityScore?.toFixed(2)}</strong> • Arrival Order: <strong>#{successResult.requestArrivalOrder}</strong> • Assigned Queue Rank: <strong>#{successResult.queueRank}</strong>. Redirecting to registry...
            </p>
          </div>
        </div>
      )}

      <div className="form-layout-grid">
        <div className="form-card">
          <form onSubmit={handleSubmit} noValidate>
            <h3 className="section-title">Sector Selection & Supply Demand</h3>

            <div className="form-group">
              <label htmlFor="regionId">Target Disaster Sector *</label>
              <select
                id="regionId"
                name="regionId"
                required
                value={formData.regionId}
                onChange={handleChange}
                disabled={submitting}
                className="filter-select"
                style={{ width: '100%', padding: '0.7rem' }}
              >
                {regions.map((reg) => (
                  <option key={reg.regionId} value={reg.regionId}>
                    {reg.regionName} (Priority Score: ★ {reg.priorityScore?.toFixed(2)})
                  </option>
                ))}
              </select>
              <span className="field-hint">The geographical disaster sector in urgent need of assistance.</span>
            </div>

            <div className="form-group">
              <label htmlFor="resourceRequired">Resource Category / Name *</label>
              <input
                id="resourceRequired"
                name="resourceRequired"
                type="text"
                required
                placeholder="e.g. Water, Food, Medicines, Ambulances"
                value={formData.resourceRequired}
                onChange={handleChange}
                disabled={submitting}
              />
              <div className="preset-badges-container">
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Quick Select:</span>
                {PRESET_RESOURCES.map((name) => (
                  <button
                    key={name}
                    type="button"
                    className={`preset-pill ${formData.resourceRequired.toLowerCase() === name.toLowerCase() ? 'active' : ''}`}
                    onClick={() => handleSelectPreset(name)}
                    tabIndex="-1"
                  >
                    {name}
                  </button>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="requiredQuantity">Required Quantity (Units) *</label>
              <input
                id="requiredQuantity"
                name="requiredQuantity"
                type="number"
                min="1"
                required
                value={formData.requiredQuantity}
                onChange={handleChange}
                disabled={submitting}
              />
              <span className="field-hint">Must be greater than 0. Units will be allocated from depots or mobile surplus.</span>
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
                    <span>Enqueueing Demand...</span>
                  </span>
                ) : (
                  'Submit Relief Demand'
                )}
              </button>
              <button
                type="button"
                className="btn btn-outline-dark"
                onClick={() => navigate('/requests')}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>

        {/* Live Triage Queue Prediction Card */}
        <div className="preview-card">
          <div className="preview-header">
            <h4>Live Priority Preview</h4>
            <span className="preview-badge">Emergency Priority Rating</span>
          </div>

          <div className="score-display-box" style={{ background: '#0f172a' }}>
            <div className="large-score-number" style={{ color: '#f87171' }}>
              ★ {selectedRegion ? selectedRegion.priorityScore?.toFixed(2) : '0.00'}
            </div>
            <div className="score-scale">Inherited Sector Priority Score</div>
          </div>

          <div className="formula-breakdown">
            <h5>Queue Placement Parameters:</h5>
            <ul className="breakdown-list">
              <li>
                <span>Selected Sector:</span>
                <strong>{selectedRegion ? selectedRegion.regionName : 'None'}</strong>
              </li>
              <li>
                <span>Severity Index:</span>
                <strong>Level {selectedRegion ? selectedRegion.disasterSeverity : '-'}/10</strong>
              </li>
              <li>
                <span>Road Accessibility:</span>
                <strong>{selectedRegion ? `${Math.round(selectedRegion.roadAccessibility * 100)}% Open` : '-'}</strong>
              </li>
              <li>
                <span>Initial Status:</span>
                <strong style={{ color: '#c2410c' }}>PENDING (0 Allocated)</strong>
              </li>
              <li>
                <span>Tie-Breaker Rule:</span>
                <strong>FIFO (Next Monotonic Sequence)</strong>
              </li>
            </ul>
          </div>

          <div className="formula-note">
            <span style={{ fontSize: '1.1rem' }}>⚖️</span>
            <span>
              The request receives the priority score of the target sector. Demands with higher priority scores are served first; demands with identical scores are served in order of arrival.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

