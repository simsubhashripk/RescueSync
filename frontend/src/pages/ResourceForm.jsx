import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchAllRegions } from '../services/regionService';
import {
  createResource,
  updateResource,
  fetchResourceById,
} from '../services/resourceService';

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
  'First Aid Kits',
];

export default function ResourceForm() {
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const navigate = useNavigate();
  const { user } = useAuth();

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const [regions, setRegions] = useState([]);
  const [formData, setFormData] = useState({
    regionId: '',
    resourceName: '',
    availableQuantity: 100,
    reservedQuantity: 20,
  });

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const surplus = Math.max(0, (Number(formData.availableQuantity) || 0) - (Number(formData.reservedQuantity) || 0));

  useEffect(() => {
    async function initForm() {
      setLoading(true);
      setError('');
      try {
        const regionList = await fetchAllRegions();
        setRegions(regionList);

        if (isEditMode) {
          const resourceData = await fetchResourceById(id);
          setFormData({
            regionId: resourceData.regionId || '',
            resourceName: resourceData.resourceName || '',
            availableQuantity: resourceData.availableQuantity ?? 0,
            reservedQuantity: resourceData.reservedQuantity ?? 0,
          });
        } else if (regionList.length > 0) {
          setFormData((prev) => ({
            ...prev,
            regionId: regionList[0].regionId,
          }));
        }
      } catch (err) {
        setError(err.message || 'Failed to initialize resource form.');
      } finally {
        setLoading(false);
      }
    }

    initForm();
  }, [id, isEditMode]);

  // Authorization Guard: Only Admins can register/edit inventory
  if (!isAdmin) {
    return (
      <div className="main-content">
        <div className="alert-box alert-error" style={{ margin: '2rem 0' }}>
          <span className="alert-icon">🚫</span>
          <div>
            <strong>Administrative Clearance Required</strong>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.9rem' }}>
              Only system administrators have permission to register or adjust regional resource inventories.
            </p>
          </div>
        </div>
        <Link to="/resources" className="btn btn-outline-dark">
          ← Return to Resource Inventory
        </Link>
      </div>
    );
  }

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    let parsedValue = value;
    if (type === 'number') {
      parsedValue = value === '' ? '' : Math.max(0, parseInt(value, 10) || 0);
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
      resourceName: name,
    }));
    if (error) setError('');
  };

  const validate = () => {
    if (!formData.regionId) {
      return 'Please select an affected disaster region.';
    }
    if (!formData.resourceName.trim()) {
      return 'Resource name is required.';
    }
    if (formData.availableQuantity < 0) {
      return 'Available quantity must be non-negative.';
    }
    if (formData.reservedQuantity < 0) {
      return 'Reserved quantity must be non-negative.';
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
      regionId: Number(formData.regionId),
      resourceName: formData.resourceName.trim(),
      availableQuantity: Number(formData.availableQuantity),
      reservedQuantity: Number(formData.reservedQuantity),
    };

    try {
      if (isEditMode) {
        await updateResource(id, payload);
        setSuccessMsg(`Resource "${payload.resourceName}" updated successfully.`);
      } else {
        await createResource(payload);
        setSuccessMsg(`Resource "${payload.resourceName}" added to sector inventory.`);
      }
      setTimeout(() => {
        navigate('/resources');
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
          <p>Loading resource item details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/resources">Resource Inventory</Link> /{' '}
            <span>{isEditMode ? `Edit Item #${id}` : 'Register Resource'}</span>
          </div>
          <h1 className="page-title">
            {isEditMode ? `Edit Resource: ${formData.resourceName}` : 'Add New Inventory Stock'}
          </h1>
          <p className="page-desc">
            Assign relief materials, medical transport, and emergency hardware to disaster sectors with designated local reserve buffers.
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
          <span>{successMsg} Redirecting to inventory...</span>
        </div>
      )}

      <div className="form-layout-grid">
        <div className="form-card">
          <form onSubmit={handleSubmit} noValidate>
            <h3 className="section-title">Regional Assignment & Material Type</h3>

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
                <option value="">-- Choose Assigned Sector --</option>
                {regions.map((reg) => (
                  <option key={reg.regionId} value={reg.regionId}>
                    {reg.regionName} ({reg.location})
                  </option>
                ))}
              </select>
              <span className="field-hint">The geographical zone warehouse where physical stock is stationed.</span>
            </div>

            <div className="form-group">
              <label htmlFor="resourceName">Resource Category / Name *</label>
              <input
                id="resourceName"
                name="resourceName"
                type="text"
                required
                placeholder="e.g. Water, Food, Medicines, Ambulances"
                value={formData.resourceName}
                onChange={handleChange}
                disabled={submitting}
              />
              <div className="preset-badges-container">
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Quick Select:</span>
                {PRESET_RESOURCES.map((name) => (
                  <button
                    key={name}
                    type="button"
                    className={`preset-pill ${formData.resourceName.toLowerCase() === name.toLowerCase() ? 'active' : ''}`}
                    onClick={() => handleSelectPreset(name)}
                    tabIndex="-1"
                  >
                    {name}
                  </button>
                ))}
              </div>
            </div>

            <h3 className="section-title" style={{ marginTop: '2rem' }}>
              Inventory Counts & Emergency Reserve
            </h3>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="availableQuantity">Available Quantity (Units) *</label>
                <input
                  id="availableQuantity"
                  name="availableQuantity"
                  type="number"
                  min="0"
                  required
                  value={formData.availableQuantity}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <span className="field-hint">Actual physical units ready in warehouse inventory.</span>
              </div>

              <div className="form-group">
                <label htmlFor="reservedQuantity">Reserved Buffer Quantity *</label>
                <input
                  id="reservedQuantity"
                  name="reservedQuantity"
                  type="number"
                  min="0"
                  required
                  value={formData.reservedQuantity}
                  onChange={handleChange}
                  disabled={submitting}
                />
                <span className="field-hint">Protected minimum stock retained for local emergency contingency.</span>
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
                    <span>Saving Inventory...</span>
                  </span>
                ) : isEditMode ? (
                  'Update Resource Record'
                ) : (
                  'Register Resource Record'
                )}
              </button>
              <button
                type="button"
                className="btn btn-outline-dark"
                onClick={() => navigate('/resources')}
                disabled={submitting}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>

        {/* Live Stock Analysis Preview */}
        <div className="preview-card">
          <div className="preview-header">
            <h4>Live Logistics Analysis</h4>
            <span className="preview-badge">Inventory Engine</span>
          </div>

          <div className="score-display-box" style={{ background: '#0f172a' }}>
            <div className="large-score-number" style={{ color: surplus > 0 ? '#4ade80' : '#f87171' }}>
              +{surplus}
            </div>
            <div className="score-scale">Surplus Units Available for Transfer</div>
          </div>

          <div className="formula-breakdown">
            <h5>Stock Partition Model:</h5>
            <ul className="breakdown-list">
              <li>
                <span>Available Physical Units:</span>
                <strong>{Number(formData.availableQuantity) || 0} units</strong>
              </li>
              <li>
                <span>Reserved Safety Buffer:</span>
                <strong>{Number(formData.reservedQuantity) || 0} units</strong>
              </li>
              <li>
                <span>Local Buffer Protection:</span>
                <strong>
                  {(Number(formData.availableQuantity) || 0) >= (Number(formData.reservedQuantity) || 0)
                    ? '100% Secured'
                    : `Deficit: ${Math.abs((Number(formData.availableQuantity) || 0) - (Number(formData.reservedQuantity) || 0))} units`}
                </strong>
              </li>
            </ul>
          </div>

          <div className="formula-note">
            <span style={{ fontSize: '1.1rem' }}>🛡️</span>
            <span>
              The Dynamic Redistribution Algorithm only mobilizes surplus stock exceeding the local safety reserve. The protected buffer prevents secondary regional shortages.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

