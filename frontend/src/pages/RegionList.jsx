import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchAllRegions, deleteRegion } from '../services/regionService';

export default function RegionList() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [deletingId, setDeletingId] = useState(null);

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const loadRegions = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchAllRegions();
      setRegions(data);
    } catch (err) {
      setError(err.message || 'Failed to load disaster areas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRegions();
  }, []);

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete "${name}"? Existing relief operations associated with this region may prevent deletion.`)) {
      return;
    }

    setDeletingId(id);
    setError('');
    setSuccessMsg('');

    try {
      const result = await deleteRegion(id);
      setSuccessMsg(result.message || `Region ${name} deleted successfully.`);
      await loadRegions();
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  };

  const filteredRegions = regions.filter((r) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      r.regionName?.toLowerCase().includes(q) ||
      r.location?.toLowerCase().includes(q)
    );
  });

  const getSeverityBadgeClass = (severity) => {
    if (severity >= 8) return 'badge-severity-critical';
    if (severity >= 6) return 'badge-severity-high';
    if (severity >= 4) return 'badge-severity-medium';
    return 'badge-severity-low';
  };

  const getPriorityBadgeClass = (score) => {
    if (score >= 50) return 'badge-priority-critical';
    if (score >= 40) return 'badge-priority-high';
    if (score >= 30) return 'badge-priority-medium';
    return 'badge-priority-low';
  };

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Dashboard</Link> / <span>Disaster Affected Regions</span>
          </div>
          <h1 className="page-title">Disaster Impacted Regions</h1>
          <p className="page-desc">
            Monitor and coordinate registered disaster sectors, casualty counts, road accessibility, and emergency priority rankings.
          </p>
        </div>
        {isAdmin && (
          <div>
            <Link to="/regions/new" className="btn btn-primary">
              <span style={{ marginRight: '0.4rem' }}>➕</span> Register Disaster Area
            </Link>
          </div>
        )}
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
          <span>{successMsg}</span>
        </div>
      )}

      <div className="filter-bar">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Search sector by name or location..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button className="clear-search-btn" onClick={() => setSearchQuery('')}>
              ✕
            </button>
          )}
        </div>
        <div className="record-count">
          Showing <strong>{filteredRegions.length}</strong> of {regions.length} registered sectors
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Retrieving regional disaster sectors...</p>
        </div>
      ) : filteredRegions.length === 0 ? (
        <div className="empty-state-card">
          <div className="empty-icon">📍</div>
          <h3>No Disaster Areas Found</h3>
          <p>
            {searchQuery
              ? `No regions match your query "${searchQuery}". Try modifying search keywords.`
              : 'There are currently no disaster-affected regions recorded in the system.'}
          </p>
          {isAdmin && !searchQuery && (
            <Link to="/regions/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>
              Register First Sector
            </Link>
          )}
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '60px' }}>ID</th>
                <th>Region Name</th>
                <th>Location</th>
                <th style={{ textAlign: 'right' }}>Affected Pop.</th>
                <th style={{ textAlign: 'center' }}>Severity</th>
                <th style={{ textAlign: 'right' }}>Injured</th>
                <th style={{ textAlign: 'center' }}>Road Access</th>
                <th style={{ textAlign: 'center' }}>Priority Score</th>
                <th style={{ textAlign: 'center', width: '160px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRegions.map((region) => (
                <tr key={region.regionId} className="table-row">
                  <td className="row-id">#{region.regionId}</td>
                  <td>
                    <Link to={`/regions/${region.regionId}`} className="region-name-link">
                      <strong>{region.regionName}</strong>
                    </Link>
                  </td>
                  <td>{region.location}</td>
                  <td style={{ textAlign: 'right' }}>
                    {region.populationAffected?.toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className={`badge ${getSeverityBadgeClass(region.disasterSeverity)}`}>
                      Level {region.disasterSeverity}/10
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <span style={{ color: region.injuredPeople > 0 ? '#b91c1c' : '#475569', fontWeight: 600 }}>
                      {region.injuredPeople?.toLocaleString()}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className="road-access-indicator" title={`Accessibility: ${(region.roadAccessibility * 100).toFixed(0)}%`}>
                      {Math.round(region.roadAccessibility * 100)}%
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className={`priority-score-badge ${getPriorityBadgeClass(region.priorityScore)}`}>
                      ★ {region.priorityScore?.toFixed(2)}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="table-actions">
                      <button
                        className="btn-action btn-action-view"
                        onClick={() => navigate(`/regions/${region.regionId}`)}
                        title="View Full Regional Analysis"
                      >
                        Inspect
                      </button>
                      {isAdmin && (
                        <>
                          <button
                            className="btn-action btn-action-edit"
                            onClick={() => navigate(`/regions/${region.regionId}/edit`)}
                            title="Edit Sector Parameters"
                          >
                            Edit
                          </button>
                          <button
                            className="btn-action btn-action-delete"
                            onClick={() => handleDelete(region.regionId, region.regionName)}
                            disabled={deletingId === region.regionId}
                            title="Safely Delete Sector"
                          >
                            {deletingId === region.regionId ? '...' : 'Del'}
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

