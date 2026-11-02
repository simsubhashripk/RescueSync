import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  fetchShortages,
  fetchSurpluses,
  fetchRecommendations,
  generateRecommendations,
  fetchRedistributionHistory,
  fetchRedistributionSummary,
  executeTransfer,
  executeBatchRedistribution
} from '../services/redistributionService';

export default function RedistributionDashboard() {
  const { user } = useAuth();
  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const [activeTab, setActiveTab] = useState('recommendations'); // 'recommendations' | 'audit' | 'history'
  const [summary, setSummary] = useState(null);
  const [shortages, setShortages] = useState([]);
  const [surpluses, setSurpluses] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Transfer action states
  const [transferringId, setTransferringId] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isBatchExecuting, setIsBatchExecuting] = useState(false);
  const [showBatchModal, setShowBatchModal] = useState(false);

  // History filters
  const [historySearch, setHistorySearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [sumData, shortList, surpList, recList, histList] = await Promise.all([
        fetchRedistributionSummary(),
        fetchShortages(),
        fetchSurpluses(),
        fetchRecommendations(),
        fetchRedistributionHistory()
      ]);
      setSummary(sumData);
      setShortages(shortList);
      setSurpluses(surpList);
      setRecommendations(recList);
      setHistory(histList);
    } catch (err) {
      console.error('Failed to load redistribution data:', err);
      setError(err.message || 'Failed to load redistribution data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleGenerateRecommendations = async () => {
    try {
      setIsGenerating(true);
      setError(null);
      setActionSuccess(null);
      const generated = await generateRecommendations();
      setActionSuccess(`Successfully generated ${generated.length} route-verified redistribution recommendation(s).`);
      await loadData();
    } catch (err) {
      console.error('Failed to generate recommendations:', err);
      setError(err.message || 'Failed to generate recommendations.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleExecuteSingleTransfer = async (recordId) => {
    try {
      setTransferringId(recordId);
      setError(null);
      setActionSuccess(null);
      const result = await executeTransfer(recordId);
      setActionSuccess(result.message);
      await loadData();
    } catch (err) {
      console.error(`Transfer #${recordId} failed:`, err);
      setError(err.message || `Transfer #${recordId} execution failed.`);
    } finally {
      setTransferringId(null);
    }
  };

  const handleConfirmBatchRedistribution = async () => {
    try {
      setIsBatchExecuting(true);
      setError(null);
      setActionSuccess(null);
      const results = await executeBatchRedistribution();
      const successCount = results.filter((r) => r.success).length;
      setActionSuccess(`Batch redistribution completed: ${successCount} of ${results.length} transfer(s) completed successfully.`);
      setShowBatchModal(false);
      await loadData();
    } catch (err) {
      console.error('Batch redistribution failed:', err);
      setError(err.message || 'Batch redistribution execution failed.');
      setShowBatchModal(false);
    } finally {
      setIsBatchExecuting(false);
    }
  };

  const filteredHistory = useMemo(() => {
    return history.filter((item) => {
      const q = historySearch.toLowerCase().trim();
      const matchSearch =
        !q ||
        item.recordId?.toString().includes(q) ||
        item.sourceRegionName?.toLowerCase().includes(q) ||
        item.destRegionName?.toLowerCase().includes(q) ||
        item.resourceName?.toLowerCase().includes(q);

      const matchStatus = statusFilter === 'ALL' || item.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [history, historySearch, statusFilter]);

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
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Home</Link> &gt; <Link to="/allocations">Allocations</Link> &gt; Resource Redistribution
          </div>
          <h1 className="page-title">Resource Redistribution</h1>
          <p className="page-subtitle">
            Automated monitoring system detecting regional supply shortages and surpluses, calculating clear transit corridors, and coordinating transfers between emergency sectors.
          </p>
        </div>
        <div className="page-actions-group">
          {isAdmin && (
            <>
              <button
                onClick={handleGenerateRecommendations}
                className="btn btn-outline-dark"
                disabled={isGenerating || loading}
                title="Generate route-verified transfer recommendations without altering stock"
              >
                {isGenerating ? (
                  <span className="btn-loading-content">
                    <span className="mini-spinner" /> Analyzing...
                  </span>
                ) : (
                  '🧭 Generate Recommendations'
                )}
              </button>
              <button
                onClick={() => setShowBatchModal(true)}
                className="btn btn-primary"
                disabled={isBatchExecuting || recommendations.length === 0}
                title={recommendations.length === 0 ? 'No active recommendations pending' : 'Execute all pending recommendations'}
              >
                🚀 Execute Batch Transfers
              </button>
            </>
          )}
          <button onClick={loadData} className="btn btn-outline-dark" title="Refresh data">
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div className="alert alert-danger" role="alert">
          <strong>Notice:</strong> {error}
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
            <span className="metric-icon">⚠️</span>
            <div className="metric-value">{summary.totalShortages}</div>
            <div className="metric-label">Regional Shortages</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">📦</span>
            <div className="metric-value">{summary.totalSurpluses}</div>
            <div className="metric-label">Exportable Surpluses</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">🧭</span>
            <div className="metric-value">{summary.recommendedTransfersCount}</div>
            <div className="metric-label">Recommended Transfers</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">✅</span>
            <div className="metric-value">{summary.completedTransfersCount}</div>
            <div className="metric-label">Completed Transfers</div>
          </div>
          <div className="metric-card">
            <span className="metric-icon">🚚</span>
            <div className="metric-value">{summary.totalUnitsTransferred.toLocaleString()}</div>
            <div className="metric-label">Units Transferred</div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '1px solid #cbd5e1', paddingBottom: '0.5rem' }}>
        <button
          onClick={() => setActiveTab('recommendations')}
          className={`btn btn-sm ${activeTab === 'recommendations' ? 'btn-primary' : 'btn-outline-dark'}`}
        >
          ⚡ Recommended Transfers ({recommendations.length})
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`btn btn-sm ${activeTab === 'audit' ? 'btn-primary' : 'btn-outline-dark'}`}
        >
          📊 Shortage &amp; Surplus Audit ({shortages.length + surpluses.length})
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`btn btn-sm ${activeTab === 'history' ? 'btn-primary' : 'btn-outline-dark'}`}
        >
          📑 Historical Transfers ({history.length})
        </button>
      </div>

      {/* TAB 1: Recommended Transfers */}
      {activeTab === 'recommendations' && (
        <div>
          {loading ? (
            <div className="loading-state">
              <div className="spinner"></div>
              <p>Analyzing redistribution corridors...</p>
            </div>
          ) : recommendations.length === 0 ? (
            <div className="empty-state card">
              <div className="empty-icon">🤝</div>
              <h3>No Pending Redistribution Recommendations</h3>
              <p>
                All regional deficits are currently covered, or no new recommendations have been generated yet.
                {isAdmin && ' Click "Generate Recommendations" above to run an automated shortage-surplus matching pass.'}
              </p>
              <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
                <Link to="/map" className="btn btn-outline-dark btn-sm">
                  🗺️ Inspect Transit Corridors
                </Link>
                <Link to="/allocations" className="btn btn-outline-dark btn-sm">
                  📑 View Allocations
                </Link>
              </div>
            </div>
          ) : (
            <div className="card table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Record ID</th>
                    <th>Donor Sector (Source)</th>
                    <th>Recipient Sector (Destination)</th>
                    <th>Resource</th>
                    <th>Transfer Quantity</th>
                    <th>Est. Travel Time</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {recommendations.map((rec) => (
                    <tr key={rec.recordId}>
                      <td>
                        <strong>#{rec.recordId}</strong>
                      </td>
                      <td>
                        <span style={{ color: '#0369a1', fontWeight: 600 }}>
                          📍 {rec.sourceRegionName || `Region #${rec.sourceRegionId}`}
                        </span>
                      </td>
                      <td>
                        <strong style={{ color: '#b91c1c' }}>
                          🎯 {rec.destRegionName || `Region #${rec.destRegionId}`}
                        </strong>
                      </td>
                      <td>
                        <span className="badge badge-primary">{rec.resourceName}</span>
                      </td>
                      <td>
                        <strong style={{ fontSize: '1rem', color: '#15803d' }}>
                          {rec.quantity} units
                        </strong>
                      </td>
                      <td>
                        ⏱️ <strong>{rec.estimatedTravelTime != null ? rec.estimatedTravelTime.toFixed(1) : '—'} mins</strong>
                      </td>
                      <td>
                        <span className="badge badge-warning" style={{ fontSize: '0.75rem' }}>
                          {rec.status}
                        </span>
                      </td>
                      <td>
                        {isAdmin ? (
                          <button
                            onClick={() => handleExecuteSingleTransfer(rec.recordId)}
                            className="btn btn-primary btn-sm"
                            disabled={transferringId === rec.recordId}
                            style={{ fontSize: '0.78rem', padding: '0.25rem 0.65rem' }}
                          >
                            {transferringId === rec.recordId ? (
                              <span className="btn-loading-content">
                                <span className="mini-spinner" /> Dispatching...
                              </span>
                            ) : (
                              '🚀 Execute Transfer'
                            )}
                          </button>
                        ) : (
                          <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>Admin Only</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Shortage & Surplus Audit */}
      {activeTab === 'audit' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
          {/* Shortages Section */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#991b1b' }}>
                  ⚠️ Critical Resource Deficits (Shortages)
                </h3>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: '#64748b' }}>
                  Formula: <code>Shortage = max(0, PendingDeficit - UsableStock)</code> • Reserved buffer is strictly protected from usable stock
                </p>
              </div>
              <span className="badge badge-danger">{shortages.length} Sectors with Shortages</span>
            </div>

            {shortages.length === 0 ? (
              <p style={{ color: '#15803d', margin: 0, fontSize: '0.9rem' }}>
                ✅ No critical shortages detected! All local demands are satisfied by usable regional stock.
              </p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Recipient Sector</th>
                      <th>Resource Type</th>
                      <th>Available Stock</th>
                      <th>Reserved Buffer</th>
                      <th>Usable Stock</th>
                      <th>Pending Deficit</th>
                      <th>Net Shortage</th>
                      <th>Triage Urgency Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {shortages.map((s, idx) => (
                      <tr key={idx}>
                        <td><strong>{s.regionName}</strong></td>
                        <td><span className="badge badge-primary">{s.resourceName}</span></td>
                        <td>{s.availableQuantity}</td>
                        <td style={{ color: '#64748b' }}>{s.reservedQuantity}</td>
                        <td><strong>{s.usableStock}</strong></td>
                        <td style={{ color: '#b91c1c' }}>{s.pendingDeficit}</td>
                        <td>
                          <strong style={{ color: '#dc2626', fontSize: '1rem' }}>
                            {s.shortageQuantity} units
                          </strong>
                        </td>
                        <td>
                          <span className="badge badge-secondary">{s.priorityScore?.toFixed(1)}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Surpluses Section */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#166534' }}>
                  📦 Available Exportable Inventory (Surpluses)
                </h3>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: '#64748b' }}>
                  Formula: <code>Surplus = max(0, Available - Reserved - LocalDemand)</code> • Protects donor emergency reserves and local requirements
                </p>
              </div>
              <span className="badge badge-success">{surpluses.length} Exportable Surplus Pools</span>
            </div>

            {surpluses.length === 0 ? (
              <p style={{ color: '#854d0e', margin: 0, fontSize: '0.9rem' }}>
                ⚠️ No regional surpluses available beyond emergency reserve buffers and local demands.
              </p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Donor Sector</th>
                      <th>Resource Type</th>
                      <th>Available Stock</th>
                      <th>Reserved Buffer (Protected)</th>
                      <th>Local Pending Demand</th>
                      <th>Exportable Surplus</th>
                    </tr>
                  </thead>
                  <tbody>
                    {surpluses.map((s, idx) => (
                      <tr key={idx}>
                        <td><strong>{s.regionName}</strong></td>
                        <td><span className="badge badge-primary">{s.resourceName}</span></td>
                        <td>{s.availableQuantity}</td>
                        <td style={{ color: '#64748b' }}>{s.reservedQuantity}</td>
                        <td>{s.pendingDeficit}</td>
                        <td>
                          <strong style={{ color: '#15803d', fontSize: '1rem' }}>
                            +{s.surplusQuantity} units
                          </strong>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: Historical Transfers */}
      {activeTab === 'history' && (
        <div>
          {/* Filter Bar */}
          <div className="inventory-controls-card">
            <div className="search-bar">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                className="search-input"
                placeholder="Search history by ID, donor, recipient, or resource..."
                value={historySearch}
                onChange={(e) => setHistorySearch(e.target.value)}
              />
              {historySearch && (
                <button onClick={() => setHistorySearch('')} className="clear-btn" title="Clear">✕</button>
              )}
            </div>

            <div className="filter-dropdowns">
              <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="RECOMMENDED">RECOMMENDED</option>
              </select>
            </div>
          </div>

          <div className="card table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Record ID</th>
                  <th>Donor Source</th>
                  <th>Recipient Destination</th>
                  <th>Resource</th>
                  <th>Quantity</th>
                  <th>Est. Travel Time</th>
                  <th>Status</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {filteredHistory.map((rec) => (
                  <tr key={rec.recordId}>
                    <td><strong>#{rec.recordId}</strong></td>
                    <td>📍 {rec.sourceRegionName || `Region #${rec.sourceRegionId}`}</td>
                    <td>🎯 {rec.destRegionName || `Region #${rec.destRegionId}`}</td>
                    <td><span className="badge badge-primary">{rec.resourceName}</span></td>
                    <td><strong>{rec.quantity} units</strong></td>
                    <td>⏱️ {rec.estimatedTravelTime != null ? rec.estimatedTravelTime.toFixed(1) : '—'} mins</td>
                    <td>
                      <span
                        className={`badge ${
                          rec.status === 'COMPLETED'
                            ? 'badge-success'
                            : rec.status === 'RECOMMENDED'
                            ? 'badge-warning'
                            : 'badge-secondary'
                        }`}
                        style={{ fontSize: '0.75rem' }}
                      >
                        {rec.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.85rem', color: '#64748b' }}>
                      {formatDate(rec.recommendedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Confirmation Modal for Batch Redistribution */}
      {showBatchModal && (
        <div className="modal-overlay" onClick={() => !isBatchExecuting && setShowBatchModal(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Confirm Batch Redistribution Execution</h3>
              <button
                className="modal-close-btn"
                onClick={() => !isBatchExecuting && setShowBatchModal(false)}
                disabled={isBatchExecuting}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p>
                You are about to execute <strong>{recommendations.length} pending redistribution transfer(s)</strong> between donor depots and disaster sectors.
              </p>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '6px',
                  padding: '0.85rem 1rem',
                  fontSize: '0.88rem',
                  marginBottom: '1rem',
                }}
              >
                <div style={{ marginBottom: '0.4rem' }}>
                  <strong>Safety Reserve Invariant:</strong> Donor stock will strictly remain &ge; reserved_quantity. Transfers are rejected if reserves would be breached.
                </div>
                <div style={{ marginBottom: '0.4rem' }}>
                  <strong>Operational Integrity:</strong> Stock updates are verified and applied atomically.
                </div>
                <div>
                  <strong>Audit Log:</strong> All executed transfers are recorded in the historical log.
                </div>
              </div>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#64748b' }}>
                Do you wish to authorize and commit this batch redistribution?
              </p>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-outline-dark"
                onClick={() => setShowBatchModal(false)}
                disabled={isBatchExecuting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleConfirmBatchRedistribution}
                disabled={isBatchExecuting}
              >
                {isBatchExecuting ? (
                  <span className="btn-loading-content">
                    <span className="mini-spinner" /> Executing Transfers...
                  </span>
                ) : (
                  '🚀 Commit Batch Redistribution'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

