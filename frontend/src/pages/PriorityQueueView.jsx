import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchPriorityQueueSummary } from '../services/requestService';

export default function PriorityQueueView() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const loadQueue = async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError('');

    try {
      const data = await fetchPriorityQueueSummary();
      setSummary(data);
    } catch (err) {
      setError(err.message || 'Failed to load active priority queue snapshot.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadQueue();
  }, []);

  const getPriorityBadgeClass = (score) => {
    if (score >= 60) return 'badge-priority-critical';
    if (score >= 45) return 'badge-priority-high';
    if (score >= 30) return 'badge-priority-medium';
    return 'badge-priority-low';
  };

  const getPriorityTier = (score) => {
    if (score >= 60) return { label: 'CRITICAL URGENCY', class: 'tier-critical' };
    if (score >= 45) return { label: 'HIGH URGENCY', class: 'tier-high' };
    if (score >= 30) return { label: 'MODERATE URGENCY', class: 'tier-medium' };
    return { label: 'STANDARD URGENCY', class: 'tier-low' };
  };

  const activeQueue = summary?.activeQueue || [];

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Dashboard</Link> / <Link to="/requests">Relief Requests</Link> / <span>Priority Queue</span>
          </div>
          <h1 className="page-title">Emergency Relief Priority Queue</h1>
          <p className="page-desc">
            Real-time operational view of prioritized emergency requests. Demands are organized by emergency priority score and incoming sequence to ensure rapid response.
          </p>
        </div>
        <div className="page-actions-group">
          <button
            onClick={() => loadQueue(true)}
            className="btn btn-outline-dark"
            disabled={loading || refreshing}
            title="Refresh priority queue"
          >
            {refreshing ? 'Refreshing...' : '🔄 Refresh Queue'}
          </button>
          <Link to="/requests" className="btn btn-outline-dark">
            ← All Requests
          </Link>
          <Link to="/requests/new" className="btn btn-primary">
            ➕ Submit Demand
          </Link>
        </div>
      </div>

      {error && (
        <div className="alert-box alert-error" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* Priority Rules Explanatory Box */}
      <div className="queue-algorithm-explainer">
        <div className="explainer-icon">📋</div>
        <div className="explainer-content">
          <h4>Emergency Allocation Priority Rules</h4>
          <p>
            Requests are dynamically organized to ensure urgent life-saving supplies reach affected populations without delay:
          </p>
          <div className="explainer-steps">
            <div className="step-pill">
              <strong>1. Priority Rating:</strong> Critical disaster severity and high-urgency sectors are served first
            </div>
            <div className="step-pill">
              <strong>2. Submission Order:</strong> Earlier submissions are fulfilled sequentially among equal priority levels
            </div>
            <div className="step-pill">
              <strong>3. Status Filter:</strong> Active pending and partial requests awaiting full fulfillment
            </div>
          </div>
        </div>
      </div>

      {/* Queue Executive Metrics */}
      {summary && (
        <div className="metric-spotlight-grid">
          <div className="spotlight-card priority-spotlight">
            <span className="spotlight-label">Active Demands in Queue</span>
            <div className="spotlight-value" style={{ color: '#dc2626' }}>
              {summary.activeQueueDemands}
            </div>
            <p className="spotlight-caption">Demands awaiting warehouse allocation or road dispatch.</p>
          </div>

          <div className="spotlight-card">
            <span className="spotlight-label">Pending Demands</span>
            <div className="spotlight-value" style={{ color: '#c2410c' }}>
              {summary.pendingCount}
            </div>
            <p className="spotlight-caption">0 units dispatched so far.</p>
          </div>

          <div className="spotlight-card">
            <span className="spotlight-label">Partially Allocated</span>
            <div className="spotlight-value" style={{ color: '#854d0e' }}>
              {summary.partiallyAllocatedCount}
            </div>
            <p className="spotlight-caption">Partial grants dispatched, remaining shortage in queue.</p>
          </div>

          <div className="spotlight-card">
            <span className="spotlight-label">Total Unmet Deficit</span>
            <div className="spotlight-value" style={{ color: '#0f766e' }}>
              {summary.totalRemainingDeficit?.toLocaleString()}
            </div>
            <p className="spotlight-caption">Total physical units required to clear the active queue.</p>
          </div>
        </div>
      )}

      {/* Live Priority Queue Table */}
      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading emergency priority queue...</p>
        </div>
      ) : activeQueue.length === 0 ? (
        <div className="empty-state-card">
          <div className="empty-icon">🎉</div>
          <h3>Active Priority Queue is Empty</h3>
          <p>
            All registered disaster demands have been fulfilled. There are no pending relief shortages in the operational triage queue.
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
                <th style={{ width: '90px', textAlign: 'center' }}>Queue Rank</th>
                <th style={{ width: '65px' }}>ID</th>
                <th>Target Disaster Sector</th>
                <th>Resource Required</th>
                <th style={{ textAlign: 'right' }}>Required Units</th>
                <th style={{ textAlign: 'right' }}>Allocated Units</th>
                <th style={{ textAlign: 'right' }}>Remaining Deficit</th>
                <th style={{ textAlign: 'center' }}>Priority Score</th>
                <th style={{ textAlign: 'center' }}>Urgency Tier</th>
                <th style={{ textAlign: 'center' }}>Arrival Seq</th>
                <th style={{ textAlign: 'center' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {activeQueue.map((item) => {
                const tier = getPriorityTier(item.priorityScore);
                const isRankOne = item.queueRank === 1;

                return (
                  <tr key={item.requestId} className={`table-row ${isRankOne ? 'rank-one-row' : ''}`}>
                    <td style={{ textAlign: 'center' }}>
                      <span className={`queue-rank-pill ${isRankOne ? 'rank-first' : ''}`}>
                        #{item.queueRank}
                      </span>
                    </td>
                    <td className="row-id">#{item.requestId}</td>
                    <td>
                      <Link to={`/regions/${item.regionId}`} className="region-name-link">
                        <strong>{item.regionName}</strong>
                      </Link>
                    </td>
                    <td>
                      <strong>{item.resourceRequired}</strong>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {item.requiredQuantity?.toLocaleString()}
                    </td>
                    <td style={{ textAlign: 'right', color: item.allocatedQuantity > 0 ? '#16a34a' : '#64748b' }}>
                      {item.allocatedQuantity?.toLocaleString()}
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 800 }}>
                      <span style={{ color: '#dc2626' }}>
                        {item.remainingDeficit?.toLocaleString()}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className={`priority-score-badge ${getPriorityBadgeClass(item.priorityScore)}`}>
                        ★ {item.priorityScore?.toFixed(2)}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className={`urgency-tier-tag ${tier.class}`}>
                        {tier.label}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center', fontFamily: 'monospace', color: '#64748b' }}>
                      Seq #{item.requestArrivalOrder}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {item.requestStatus === 'PENDING' ? (
                        <span className="badge badge-stock-critical">⏳ PENDING</span>
                      ) : (
                        <span className="badge badge-stock-buffer">⚡ PARTIAL ({item.allocatedQuantity})</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

