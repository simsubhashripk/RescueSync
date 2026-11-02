import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  fetchAllAllocations,
  fetchAllocationSummary,
  executeBatchAllocation,
  allocateSingleRequest
} from '../services/allocationService';
import { fetchAllRegions } from '../services/regionService';

export default function AllocationDashboard() {
  const { user } = useAuth();
  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const [allocations, setAllocations] = useState([]);
  const [summary, setSummary] = useState(null);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Filter states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRegion, setSelectedRegion] = useState('ALL');
  const [selectedResource, setSelectedResource] = useState('ALL');

  // Confirmation modal state
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [batchResult, setBatchResult] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [allocList, summaryData, regList] = await Promise.all([
        fetchAllAllocations(),
        fetchAllocationSummary(),
        fetchAllRegions()
      ]);
      setAllocations(allocList);
      setSummary(summaryData);
      setRegions(regList);
    } catch (err) {
      console.error('Failed to load allocation data:', err);
      setError(err.message || 'Failed to load allocation records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleConfirmBatchAllocation = async () => {
    try {
      setIsExecuting(true);
      setError(null);
      setActionSuccess(null);
      const result = await executeBatchAllocation();
      setBatchResult(result);
      setActionSuccess(
        `Batch Allocation Completed! Successfully processed ${result.processedCount} active demand(s): ${result.successfulAllocations} allocated (${result.totalUnitsAllocated} total units), ${result.unfulfilledRequests} unfulfilled.`
      );
      setShowConfirmModal(false);
      // Reload updated allocation, request, and stock data
      await loadData();
    } catch (err) {
      console.error('Batch allocation failed:', err);
      setError(err.message || 'Execution of batch allocation failed.');
      setShowConfirmModal(false);
    } finally {
      setIsExecuting(false);
    }
  };

  // Distinct resource names for filter
  const resourceOptions = useMemo(() => {
    const set = new Set();
    allocations.forEach((a) => {
      if (a.resourceName) set.add(a.resourceName);
    });
    return Array.from(set).sort();
  }, [allocations]);

  // Filtered allocations
  const filteredAllocations = useMemo(() => {
    return allocations.filter((item) => {
      const q = searchQuery.toLowerCase().trim();
      const matchSearch =
        !q ||
        item.allocationId?.toString().includes(q) ||
        item.requestId?.toString().includes(q) ||
        item.regionName?.toLowerCase().includes(q) ||
        item.resourceName?.toLowerCase().includes(q) ||
        item.supplyRegionName?.toLowerCase().includes(q) ||
        item.allocatedBy?.toLowerCase().includes(q);

      const matchRegion =
        selectedRegion === 'ALL' || item.regionId?.toString() === selectedRegion;

      const matchResource =
        selectedResource === 'ALL' ||
        item.resourceName?.toLowerCase() === selectedResource.toLowerCase();

      return matchSearch && matchRegion && matchResource;
    });
  }, [allocations, searchQuery, selectedRegion, selectedResource]);

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleString([], {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="main-content">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Home</Link> &gt; <Link to="/requests">Relief Demands</Link> &gt; Resource Allocations
          </div>
          <h1 className="page-title">Resource Allocation &amp; Distribution</h1>
          <p className="page-subtitle">
            Auditable distribution engine dispatching relief inventory from regional depots to priority disaster zones.
          </p>
        </div>
        <div className="page-actions-group">
          {isAdmin && (
            <button
              onClick={() => setShowConfirmModal(true)}
              className="btn btn-primary"
              disabled={isExecuting || (summary && summary.activeQueueCount === 0)}
              title={
                summary && summary.activeQueueCount === 0
                  ? 'No active pending demands in the queue'
                  : 'Execute priority allocation batch'
              }
            >
              ⚡ Run Priority Allocation Batch
            </button>
          )}
          <button onClick={loadData} className="btn btn-outline-dark" title="Refresh allocation records">
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div className="alert alert-danger" role="alert">
          <strong>Error:</strong> {error}
        </div>
      )}

      {actionSuccess && (
        <div className="alert alert-success" role="alert">
          <strong>Success:</strong> {actionSuccess}
        </div>
      )}

      {/* Metric Cards */}
      {summary && (
        <div className="metrics-grid">
          <div className="metric-card">
            <span className="metric-icon">📑</span>
            <div className="metric-value">{summary.totalAllocations}</div>
            <div className="metric-label">Allocation Dispatches</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">📦</span>
            <div className="metric-value">{summary.totalUnitsAllocated.toLocaleString()}</div>
            <div className="metric-label">Total Units Dispatched</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">⚡</span>
            <div className="metric-value">{summary.activeQueueCount}</div>
            <div className="metric-label">Active Queue Demands</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">⏳</span>
            <div className="metric-value">{summary.partiallyAllocatedRequestsCount}</div>
            <div className="metric-label">Partially Fulfilled</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">✅</span>
            <div className="metric-value">{summary.fullyAllocatedRequestsCount}</div>
            <div className="metric-label">Fully Satisfied</div>
          </div>
        </div>
      )}

      {/* Resource Distribution Summary */}
      {summary && summary.resourceDistribution && Object.keys(summary.resourceDistribution).length > 0 && (
        <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
          <strong style={{ fontSize: '0.9rem', color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Resource Type Distribution (Units Dispatched)
          </strong>
          <div className="category-pills-list" style={{ marginTop: '0.75rem' }}>
            {Object.entries(summary.resourceDistribution).map(([resName, count]) => (
              <span key={resName} className="category-pill" style={{ fontWeight: 600 }}>
                {resName}: <strong style={{ color: '#0f172a' }}>{count.toLocaleString()} units</strong>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Batch Result Details Card if present */}
      {batchResult && batchResult.details && (
        <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem', borderLeft: '4px solid #0284c7' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <strong>Batch Execution Breakdown</strong>
            <button
              onClick={() => setBatchResult(null)}
              className="btn btn-outline-dark btn-sm"
              style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
            >
              Dismiss
            </button>
          </div>
          <ul style={{ margin: 0, paddingLeft: '1.25rem', fontSize: '0.88rem', color: '#334155' }}>
            {batchResult.details.map((item, idx) => (
              <li key={idx} style={{ marginBottom: '0.35rem' }}>
                <strong>Request #{item.requestId}:</strong>{' '}
                <span style={{ color: item.success ? '#15803d' : '#b45309' }}>
                  {item.message}
                </span>{' '}
                {item.success && (
                  <span>
                    (Allocated: <strong>{item.allocatedQuantity}</strong> | Remaining: {item.remainingDeficit} | Status: <code>{item.requestStatus}</code>)
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Controls & Filter Bar */}
      <div className="inventory-controls-card">
        <div className="search-bar">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            className="search-input"
            placeholder="Search by ID, request, region, resource, or officer..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} className="clear-btn" title="Clear query">
              ✕
            </button>
          )}
        </div>

        <div className="filter-dropdowns">
          <select
            className="filter-select"
            value={selectedRegion}
            onChange={(e) => setSelectedRegion(e.target.value)}
          >
            <option value="ALL">All Destination Sectors</option>
            {regions.map((r) => (
              <option key={r.regionId} value={r.regionId}>
                {r.regionName}
              </option>
            ))}
          </select>

          <select
            className="filter-select"
            value={selectedResource}
            onChange={(e) => setSelectedResource(e.target.value)}
          >
            <option value="ALL">All Resource Types</option>
            {resourceOptions.map((res) => (
              <option key={res} value={res}>
                {res}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Allocation Records Table */}
      {loading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading allocation records...</p>
        </div>
      ) : filteredAllocations.length === 0 ? (
        <div className="empty-state card">
          <div className="empty-icon">📦</div>
          <h3>No Allocation Records Found</h3>
          <p>
            {searchQuery || selectedRegion !== 'ALL' || selectedResource !== 'ALL'
              ? 'No dispatch records match your current filter criteria.'
              : 'No resources have been allocated yet. Run the priority allocation batch or submit relief demands.'}
          </p>
          <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
            <Link to="/requests" className="btn btn-primary btn-sm">
              View Relief Demands
            </Link>
            <Link to="/resources" className="btn btn-outline-dark btn-sm">
              Inspect Stock Depots
            </Link>
          </div>
        </div>
      ) : (
        <div className="card table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Request ID</th>
                <th>Recipient Sector</th>
                <th>Resource</th>
                <th>Quantity Dispatched</th>
                <th>Supply Depot</th>
                <th>Transit Distance</th>
                <th>Allocated By</th>
                <th>Date &amp; Time</th>
              </tr>
            </thead>
            <tbody>
              {filteredAllocations.map((alloc) => (
                <tr key={alloc.allocationId}>
                  <td>
                    <strong>#{alloc.allocationId}</strong>
                  </td>
                  <td>
                    <Link
                      to="/requests"
                      style={{ color: '#0284c7', textDecoration: 'none', fontWeight: 600 }}
                      title="View in Relief Demands"
                    >
                      Req #{alloc.requestId}
                    </Link>
                  </td>
                  <td>
                    <strong>{alloc.regionName || `Region #${alloc.regionId}`}</strong>
                  </td>
                  <td>
                    <span className="badge badge-primary">{alloc.resourceName}</span>
                  </td>
                  <td>
                    <strong style={{ fontSize: '1rem', color: '#15803d' }}>
                      {alloc.quantityAllocated} units
                    </strong>
                  </td>
                  <td>
                    {alloc.supplyRegionName ? (
                      alloc.supplyRegionId === alloc.regionId ? (
                        <span style={{ color: '#475569' }}>📍 Local Regional Stock</span>
                      ) : (
                        <span style={{ color: '#0f172a', fontWeight: 500 }}>
                          🚚 {alloc.supplyRegionName}
                        </span>
                      )
                    ) : (
                      'Local Depot'
                    )}
                  </td>
                  <td>
                    {alloc.routeDistanceKm != null && alloc.routeDistanceKm > 0 ? (
                      `${alloc.routeDistanceKm.toFixed(1)} km`
                    ) : (
                      <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>Direct / Local Transit</span>
                    )}
                  </td>
                  <td>
                    <span className="badge badge-secondary" style={{ fontSize: '0.75rem' }}>
                      {alloc.allocatedBy}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    {formatDate(alloc.allocatedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="modal-overlay" onClick={() => !isExecuting && setShowConfirmModal(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Confirm Resource Allocation Batch</h3>
              <button
                className="modal-close-btn"
                onClick={() => !isExecuting && setShowConfirmModal(false)}
                disabled={isExecuting}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p>
                You are about to execute the <strong>Resource Allocation</strong> across all active
                demands in the queue.
              </p>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '6px',
                  padding: '0.85rem 1rem',
                  fontSize: '0.88rem',
                  marginBottom: '1rem'
                }}
              >
                <div style={{ marginBottom: '0.4rem' }}>
                  <strong>Priority Evaluation:</strong> Processed in order of disaster severity and emergency priority.
                </div>
                <div style={{ marginBottom: '0.4rem' }}>
                  <strong>Safety Reserve Protection:</strong> Emergency reserves and safety buffer quantities remain protected at all times.
                </div>
                <div>
                  <strong>Audit Trail:</strong> Each allocation is recorded with complete transit and fulfillment details.
                </div>
              </div>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#64748b' }}>
                Are you sure you want to proceed with this batch dispatch?
              </p>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-outline-dark"
                onClick={() => setShowConfirmModal(false)}
                disabled={isExecuting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleConfirmBatchAllocation}
                disabled={isExecuting}
              >
                {isExecuting ? (
                  <span className="btn-loading-content">
                    <span className="mini-spinner" /> Allocating...
                  </span>
                ) : (
                  '⚡ Execute Allocation'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
