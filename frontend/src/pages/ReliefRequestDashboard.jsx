import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  fetchAllRequests,
  fetchPriorityQueueSummary,
  deleteReliefRequest,
} from '../services/requestService';
import { fetchAllRegions } from '../services/regionService';

export default function ReliefRequestDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [requests, setRequests] = useState([]);
  const [summary, setSummary] = useState(null);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRegionId, setSelectedRegionId] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [deletingId, setDeletingId] = useState(null);

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const loadData = async (isManualRefresh = false) => {
    if (isManualRefresh) setRefreshing(true);
    else setLoading(true);
    setError('');

    try {
      const [allReqs, queueSummary, regionList] = await Promise.all([
        fetchAllRequests(),
        fetchPriorityQueueSummary(),
        fetchAllRegions(),
      ]);
      setRequests(allReqs);
      setSummary(queueSummary);
      setRegions(regionList);
    } catch (err) {
      setError(err.message || 'Failed to load relief demands.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDelete = async (id, resource, regionName) => {
    if (
      !window.confirm(
        `Are you sure you want to delete Request #${id} (${resource} for ${regionName})? Associated relief allocations may prevent deletion.`
      )
    ) {
      return;
    }

    setDeletingId(id);
    setError('');
    setSuccessMsg('');

    try {
      const res = await deleteReliefRequest(id);
      setSuccessMsg(res.message || `Request #${id} deleted successfully.`);
      await loadData(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="badge badge-stock-critical">⏳ PENDING</span>;
      case 'PARTIALLY_ALLOCATED':
        return <span className="badge badge-stock-buffer">⚡ PARTIALLY ALLOCATED</span>;
      case 'ALLOCATED':
        return <span className="badge badge-stock-surplus">✓ FULFILLED</span>;
      default:
        return <span className="badge badge-priority-low">{status}</span>;
    }
  };

  const getPriorityBadgeClass = (score) => {
    if (score >= 60) return 'badge-priority-critical';
    if (score >= 45) return 'badge-priority-high';
    if (score >= 30) return 'badge-priority-medium';
    return 'badge-priority-low';
  };

  const filteredRequests = requests.filter((r) => {
    const q = searchQuery.toLowerCase().trim();
    if (q) {
      const matchResource = r.resourceRequired?.toLowerCase().includes(q);
      const matchRegion = r.regionName?.toLowerCase().includes(q);
      const matchId = String(r.requestId) === q;
      if (!matchResource && !matchRegion && !matchId) return false;
    }

    if (selectedRegionId !== 'ALL' && String(r.regionId) !== String(selectedRegionId)) {
      return false;
    }

    if (statusFilter !== 'ALL' && r.requestStatus !== statusFilter) {
      return false;
    }

    return true;
  });

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Dashboard</Link> / <span>Relief Requests</span>
          </div>
          <h1 className="page-title">Relief Demand Registry</h1>
          <p className="page-desc">
            Track emergency relief requests, incoming demands from disaster zones, and priority rankings for rapid fulfillment.
          </p>
        </div>
        <div className="page-actions-group">
          <button
            onClick={() => loadData(true)}
            className="btn btn-outline-dark"
            disabled={loading || refreshing}
            title="Refresh relief demands"
          >
            {refreshing ? 'Refreshing...' : '🔄 Refresh'}
          </button>
          <Link to="/requests/queue" className="btn btn-outline-dark">
            ⚡ View Priority Queue
          </Link>
          <Link to="/allocations" className="btn btn-outline-dark">
            📑 Allocations
          </Link>
          <Link to="/requests/new" className="btn btn-primary">
            <span style={{ marginRight: '0.4rem' }}>➕</span> Submit Relief Demand
          </Link>
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
          <span>{successMsg}</span>
        </div>
      )}

      {/* Top-Priority Spotlight Card */}
      {summary && summary.topPriorityRequest && (
        <div className="top-priority-banner">
          <div className="top-priority-icon">🚨</div>
          <div className="top-priority-details">
            <div className="top-priority-header">
              <span className="queue-rank-badge rank-1">PRIORITY RANK #1 • HIGHEST EMERGENCY DEMAND</span>
              <span className="priority-score-badge badge-priority-critical">
                ★ {summary.topPriorityRequest.priorityScore?.toFixed(2)} Urgency Score
              </span>
            </div>
            <h3>
              {summary.topPriorityRequest.resourceRequired} Demand for {summary.topPriorityRequest.regionName}
            </h3>
            <p>
              Request #{summary.topPriorityRequest.requestId} requires{' '}
              <strong>{summary.topPriorityRequest.requiredQuantity?.toLocaleString()} units</strong> (
              <span style={{ color: '#dc2626', fontWeight: 700 }}>
                {summary.topPriorityRequest.remainingDeficit?.toLocaleString()} units remaining deficit
              </span>
              ). Status: <strong>{summary.topPriorityRequest.requestStatus}</strong> • Arrival Sequence #{summary.topPriorityRequest.requestArrivalOrder}.
            </p>
          </div>
          <div className="top-priority-action">
            <Link to="/requests/queue" className="btn btn-primary btn-sm">
              Inspect Active Queue →
            </Link>
          </div>
        </div>
      )}

      {/* Metric Spotlight Grid */}
      {summary && (
        <div className="metric-spotlight-grid">
          <div className="spotlight-card">
            <span className="spotlight-label">Total Demands</span>
            <div className="spotlight-value">{summary.totalRequests}</div>
            <p className="spotlight-caption">All recorded demands across crisis sectors.</p>
          </div>

          <div className="spotlight-card priority-spotlight">
            <span className="spotlight-label">Active Queue Demands</span>
            <div className="spotlight-value" style={{ color: '#dc2626' }}>
              {summary.activeQueueDemands}
            </div>
            <p className="spotlight-caption">Pending and partial requests awaiting allocation.</p>
          </div>

          <div className="spotlight-card">
            <span className="spotlight-label">Total Required Units</span>
            <div className="spotlight-value" style={{ color: '#0f766e' }}>
              {summary.totalRequiredUnits?.toLocaleString()}
            </div>
            <p className="spotlight-caption">Aggregate supply demand across all sectors.</p>
          </div>

          <div className="spotlight-card">
            <span className="spotlight-label">Remaining Net Deficit</span>
            <div className="spotlight-value" style={{ color: '#b45309' }}>
              {summary.totalRemainingDeficit?.toLocaleString()}
            </div>
            <p className="spotlight-caption">Unmet units awaiting warehouse dispatch or inter-sector transit.</p>
          </div>
        </div>
      )}

      {/* Breakdown Summary Pills */}
      {summary && (
        <div className="category-summary-container">
          <span className="category-summary-title">Demand Status Breakdown:</span>
          <div className="category-pills-list">
            <span className="category-pill">
              <strong>Pending:</strong> {summary.pendingCount}
            </span>
            <span className="category-pill">
              <strong>Partially Allocated:</strong> {summary.partiallyAllocatedCount}
            </span>
            <span className="category-pill">
              <strong>Fully Allocated:</strong> {summary.allocatedCount}
            </span>
            <span className="category-pill">
              <strong>Units Allocated:</strong> {summary.totalAllocatedUnits?.toLocaleString()} / {summary.totalRequiredUnits?.toLocaleString()}
            </span>
          </div>
        </div>
      )}

      {/* Search and Filters */}
      <div className="filter-bar">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Search by resource, sector or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button className="clear-search-btn" onClick={() => setSearchQuery('')}>
              ✕
            </button>
          )}
        </div>

        <div className="filter-dropdowns">
          <select
            value={selectedRegionId}
            onChange={(e) => setSelectedRegionId(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Disaster Sectors</option>
            {regions.map((reg) => (
              <option key={reg.regionId} value={reg.regionId}>
                {reg.regionName}
              </option>
            ))}
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Request Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="PARTIALLY_ALLOCATED">PARTIALLY ALLOCATED</option>
            <option value="ALLOCATED">ALLOCATED (Fulfilled)</option>
          </select>
        </div>

        <div className="record-count">
          Showing <strong>{filteredRequests.length}</strong> of {requests.length} demands
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Retrieving relief demand records and priority queue...</p>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="empty-state-card">
          <div className="empty-icon">📋</div>
          <h3>No Relief Requests Found</h3>
          <p>
            {searchQuery || selectedRegionId !== 'ALL' || statusFilter !== 'ALL'
              ? 'No requests match your current search and filter settings. Try adjusting your filter parameters.'
              : 'There are currently no relief demands logged in the system.'}
          </p>
          <Link to="/requests/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            Submit New Demand
          </Link>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '65px' }}>ID</th>
                <th style={{ width: '100px', textAlign: 'center' }}>Queue Rank</th>
                <th>Target Sector</th>
                <th>Resource Required</th>
                <th style={{ textAlign: 'right' }}>Required</th>
                <th style={{ textAlign: 'right' }}>Allocated</th>
                <th style={{ textAlign: 'right' }}>Deficit</th>
                <th style={{ textAlign: 'center' }}>Priority Score</th>
                <th style={{ textAlign: 'center' }}>Arrival Seq</th>
                <th style={{ textAlign: 'center' }}>Status</th>
                <th style={{ textAlign: 'center' }}>Created</th>
                {isAdmin && <th style={{ textAlign: 'center', width: '90px' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map((req) => (
                <tr key={req.requestId} className="table-row">
                  <td className="row-id">#{req.requestId}</td>
                  <td style={{ textAlign: 'center' }}>
                    {req.queueRank != null ? (
                      <span className={`queue-rank-pill ${req.queueRank === 1 ? 'rank-first' : ''}`}>
                        #{req.queueRank}
                      </span>
                    ) : (
                      <span className="queue-rank-archived" title="Fulfilled and archived from queue">
                        ✓ Done
                      </span>
                    )}
                  </td>
                  <td>
                    <Link to={`/regions/${req.regionId}`} className="region-name-link">
                      <strong>{req.regionName}</strong>
                    </Link>
                  </td>
                  <td>
                    <strong>{req.resourceRequired}</strong>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    {req.requiredQuantity?.toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right', color: req.allocatedQuantity > 0 ? '#16a34a' : '#64748b' }}>
                    {req.allocatedQuantity?.toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 700 }}>
                    <span style={{ color: req.remainingDeficit > 0 ? '#dc2626' : '#16a34a' }}>
                      {req.remainingDeficit?.toLocaleString()}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className={`priority-score-badge ${getPriorityBadgeClass(req.priorityScore)}`}>
                      ★ {req.priorityScore?.toFixed(2)}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center', fontFamily: 'monospace', color: '#64748b' }}>
                    #{req.requestArrivalOrder}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {getStatusBadge(req.requestStatus)}
                  </td>
                  <td style={{ textAlign: 'center', fontSize: '0.8rem', color: '#64748b' }}>
                    {req.createdAt ? new Date(req.createdAt).toLocaleDateString() : 'Baseline'}
                  </td>
                  {isAdmin && (
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="btn-action btn-action-delete"
                        onClick={() => handleDelete(req.requestId, req.resourceRequired, req.regionName)}
                        disabled={deletingId === req.requestId}
                        title="Delete Request (Safety Protected)"
                      >
                        {deletingId === req.requestId ? '...' : 'Del'}
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

