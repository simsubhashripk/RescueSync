import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { useAuth } from '../context/AuthContext';
import {
  fetchAdminSummary,
  fetchRegionAnalytics,
  fetchRescueTeams
} from '../services/analyticsService';
import { fetchAllResources } from '../services/resourceService';
import { fetchAllRequests } from '../services/requestService';
import { fetchAllAllocations } from '../services/allocationService';
import { fetchRedistributionHistory } from '../services/redistributionService';

export default function AdminAnalyticsDashboard() {
  const { user } = useAuth();
  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const [activeTab, setActiveTab] = useState('kpis'); // 'kpis' | 'charts' | 'regions' | 'reports'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Real backend dataset states
  const [summary, setSummary] = useState(null);
  const [regionAnalytics, setRegionAnalytics] = useState([]);
  const [rescueTeams, setRescueTeams] = useState([]);
  const [resources, setResources] = useState([]);
  const [requests, setRequests] = useState([]);
  const [allocations, setAllocations] = useState([]);
  const [redistributions, setRedistributions] = useState([]);

  const loadAllData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [
        sumData,
        regData,
        teamsData,
        resData,
        reqData,
        allocData,
        redistData
      ] = await Promise.all([
        fetchAdminSummary(),
        fetchRegionAnalytics(),
        fetchRescueTeams(),
        fetchAllResources(),
        fetchAllRequests(),
        fetchAllAllocations(),
        fetchRedistributionHistory()
      ]);

      setSummary(sumData);
      setRegionAnalytics(regData);
      setRescueTeams(teamsData);
      setResources(resData);
      setRequests(reqData);
      setAllocations(allocData);
      setRedistributions(redistData);
    } catch (err) {
      console.error('Failed to load operations analytics:', err);
      setError(err.message || 'Failed to load emergency analytics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  // Chart 1: Resource distribution by region (Available vs Reserved)
  const resourceDistributionData = useMemo(() => {
    return regionAnalytics.map((r) => ({
      name: r.regionName,
      available: r.totalAvailableStock || 0,
      reserved: r.totalReservedStock || 0
    }));
  }, [regionAnalytics]);

  // Chart 2: Disaster severity & priority by region
  const severityPriorityData = useMemo(() => {
    return regionAnalytics.map((r) => ({
      name: r.regionName,
      severity: r.disasterSeverity || 0,
      priority: Math.round((r.priorityScore || 0) * 10) / 10
    }));
  }, [regionAnalytics]);

  // Chart 3: Relief request status breakdown
  const requestStatusData = useMemo(() => {
    const counts = { PENDING: 0, PARTIALLY_ALLOCATED: 0, ALLOCATED: 0 };
    requests.forEach((req) => {
      const st = req.requestStatus || 'PENDING';
      if (counts[st] !== undefined) {
        counts[st]++;
      } else {
        counts.PENDING++;
      }
    });

    return [
      { name: 'Pending Demands', value: counts.PENDING, color: '#dc2626' },
      { name: 'Partially Satisfied', value: counts.PARTIALLY_ALLOCATED, color: '#f59e0b' },
      { name: 'Fully Satisfied', value: counts.ALLOCATED, color: '#10b981' }
    ].filter((item) => item.value > 0);
  }, [requests]);

  // Chart 4: Redistribution volume by resource category
  const redistributionVolumeData = useMemo(() => {
    const volumeMap = {};
    redistributions
      .filter((r) => r.status === 'COMPLETED')
      .forEach((r) => {
        const res = r.resourceName || 'Unknown';
        volumeMap[res] = (volumeMap[res] || 0) + (r.quantity || 0);
      });

    return Object.entries(volumeMap).map(([name, units]) => ({
      resource: name,
      units
    }));
  }, [redistributions]);

  // Chart 5: Allocation metrics by recipient sector
  const allocationLogisticsData = useMemo(() => {
    const byRegion = {};
    allocations.forEach((a) => {
      const reg = a.regionName || `Region #${a.regionId}`;
      if (!byRegion[reg]) {
        byRegion[reg] = { name: reg, units: 0, dispatches: 0 };
      }
      byRegion[reg].units += a.quantityAllocated || 0;
      byRegion[reg].dispatches += 1;
    });
    return Object.values(byRegion);
  }, [allocations]);

  // CSV Generator Helper
  const downloadCSV = (filename, headers, rows) => {
    const escapeCell = (cell) => {
      if (cell == null) return '""';
      const str = String(cell).replace(/"/g, '""');
      return `"${str}"`;
    };

    const headerLine = headers.map(escapeCell).join(',');
    const rowLines = rows.map((r) => r.map(escapeCell).join(','));
    const csvContent = [headerLine, ...rowLines].join('\r\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportRegionReport = () => {
    const headers = [
      'Region ID',
      'Region Name',
      'Location',
      'Disaster Severity (1-10)',
      'Population Impacted',
      'Casualties / Injured',
      'Vulnerable Count',
      'Road Accessibility (%)',
      'Priority Score',
      'Available Stock',
      'Reserved Buffer',
      'Pending Deficit',
      'Fulfillment Rate (%)',
      'Assigned Teams'
    ];
    const rows = regionAnalytics.map((r) => [
      r.regionId,
      r.regionName,
      r.location,
      r.disasterSeverity,
      r.populationAffected,
      r.injuredPeople,
      r.vulnerablePeople,
      `${Math.round((r.roadAccessibility || 0) * 100)}%`,
      r.priorityScore?.toFixed(2),
      r.totalAvailableStock,
      r.totalReservedStock,
      r.pendingRequiredUnits,
      `${r.fulfillmentPercentage}%`,
      (r.assignedTeams || []).join('; ')
    ]);
    downloadCSV(`rescuesync_regions_report_${Date.now()}.csv`, headers, rows);
  };

  const exportInventoryReport = () => {
    const headers = [
      'Item ID',
      'Resource Name',
      'Region ID',
      'Sector Name',
      'Available Quantity',
      'Reserved Quantity',
      'Surplus for Redistribution'
    ];
    const rows = resources.map((r) => [
      r.resourceId,
      r.resourceName,
      r.regionId,
      r.regionName,
      r.availableQuantity,
      r.reservedQuantity,
      r.surplusQuantity
    ]);
    downloadCSV(`rescuesync_inventory_report_${Date.now()}.csv`, headers, rows);
  };

  const exportDemandsReport = () => {
    const headers = [
      'Request ID',
      'Sector Name',
      'Resource Required',
      'Required Quantity',
      'Allocated Quantity',
      'Remaining Deficit',
      'Status',
      'Priority Score',
      'Arrival Sequence'
    ];
    const rows = requests.map((r) => [
      r.requestId,
      r.regionName,
      r.resourceRequired,
      r.requiredQuantity,
      r.allocatedQuantity,
      r.remainingDeficit,
      r.requestStatus,
      r.priorityScore?.toFixed(2),
      r.requestArrivalOrder
    ]);
    downloadCSV(`rescuesync_demands_report_${Date.now()}.csv`, headers, rows);
  };

  const exportAllocationReport = () => {
    const headers = [
      'Allocation ID',
      'Request ID',
      'Recipient Sector',
      'Resource',
      'Quantity Dispatched',
      'Supply Depot',
      'Distance (km)',
      'Travel Time (mins)',
      'Allocated By',
      'Allocated Date'
    ];
    const rows = allocations.map((a) => [
      a.allocationId,
      a.requestId,
      a.regionName,
      a.resourceName,
      a.quantityAllocated,
      a.supplyRegionName || 'Local Stock',
      a.routeDistanceKm?.toFixed(1) || '0.0',
      a.routeTravelTimeMinutes?.toFixed(1) || '0.0',
      a.allocatedBy,
      a.allocatedAt
    ]);
    downloadCSV(`rescuesync_allocations_report_${Date.now()}.csv`, headers, rows);
  };

  const exportRedistributionReport = () => {
    const headers = [
      'Record ID',
      'Donor Source',
      'Recipient Destination',
      'Resource Name',
      'Quantity',
      'Estimated Transit (mins)',
      'Status',
      'Recommended Timestamp'
    ];
    const rows = redistributions.map((r) => [
      r.recordId,
      r.sourceRegionName,
      r.destRegionName,
      r.resourceName,
      r.quantity,
      r.estimatedTravelTime?.toFixed(1) || '0.0',
      r.status,
      r.recommendedAt
    ]);
    downloadCSV(`rescuesync_redistribution_report_${Date.now()}.csv`, headers, rows);
  };

  return (
    <div className="main-content">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Home</Link> &gt; Operations Analytics &amp; Reports
          </div>
          <h1 className="page-title">Operations Analytics &amp; Reports</h1>
          <p className="page-subtitle">
            Comprehensive system analytics, regional relief coverage metrics, rescue team status, and auditable operational exports.
          </p>
        </div>
        <div className="page-actions-group">
          <button onClick={loadAllData} className="btn btn-outline-dark" title="Refresh analytics data">
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

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '1px solid #cbd5e1', paddingBottom: '0.5rem', flexWrap: 'wrap' }}>
        <button
          className={`btn btn-sm ${activeTab === 'kpis' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('kpis')}
        >
          📊 Executive Overview &amp; Teams
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'charts' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('charts')}
        >
          📈 Operations Visualizations
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'regions' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('regions')}
        >
          📍 Regional Impact &amp; Coverage
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'reports' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('reports')}
        >
          📥 Operational Reports &amp; Exports
        </button>
      </div>

      {loading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Compiling real-time disaster response metrics...</p>
        </div>
      ) : (
        <>
          {/* TAB 1: EXECUTIVE OVERVIEW & RESCUE TEAMS */}
          {activeTab === 'kpis' && summary && (
            <div>
              {/* Overall Response Status Banner */}
              <div
                style={{
                  padding: '1.25rem 1.5rem',
                  borderRadius: '8px',
                  background:
                    summary.overallDisasterStatus?.includes('CRITICAL')
                      ? '#fff1f2'
                      : '#f0fdf4',
                  border:
                    summary.overallDisasterStatus?.includes('CRITICAL')
                      ? '1px solid #fecdd3'
                      : '1px solid #bbf7d0',
                  marginBottom: '1.5rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '1rem'
                }}
              >
                <div>
                  <span
                    style={{
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      color: summary.overallDisasterStatus?.includes('CRITICAL')
                        ? '#be123c'
                        : '#15803d'
                    }}
                  >
                    System Alert Status
                  </span>
                  <h2
                    style={{
                      margin: '0.25rem 0',
                      fontSize: '1.4rem',
                      color: summary.overallDisasterStatus?.includes('CRITICAL')
                        ? '#9f1239'
                        : '#166534'
                    }}
                  >
                    {summary.overallDisasterStatus}
                  </h2>
                  <p style={{ margin: 0, fontSize: '0.88rem', color: '#475569' }}>
                    Highest regional severity recorded in{' '}
                    <strong>{summary.highestSeverityRegion}</strong> (Level{' '}
                    {summary.highestSeverityLevel} / 10). Total affected population across sectors:{' '}
                    <strong>{summary.totalPopulationAffected?.toLocaleString()} persons</strong>.
                  </p>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Link to="/requests/queue" className="btn btn-primary btn-sm">
                    ⚡ View Priority Queue
                  </Link>
                  <Link to="/redistribution" className="btn btn-outline-dark btn-sm">
                    🔄 Open Redistribution
                  </Link>
                </div>
              </div>

              {/* 7 High-Level KPI Cards */}
              <div className="metrics-grid">
                <div className="metric-card">
                  <span className="metric-icon">📍</span>
                  <div className="metric-value">{summary.totalAffectedRegions}</div>
                  <div className="metric-label">Affected Sectors</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">📦</span>
                  <div className="metric-value">{summary.totalAvailableResources.toLocaleString()}</div>
                  <div className="metric-label">Available Units</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">🔒</span>
                  <div className="metric-value">{summary.totalReservedResources.toLocaleString()}</div>
                  <div className="metric-label">Reserved Emergency Buffers</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">⏳</span>
                  <div className="metric-value" style={{ color: '#dc2626' }}>
                    {summary.totalPendingRequests}
                  </div>
                  <div className="metric-label">Active Unmet Demands</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">📑</span>
                  <div className="metric-value">{summary.totalCompletedAllocations}</div>
                  <div className="metric-label">Completed Allocations</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">🚒</span>
                  <div className="metric-value">
                    {summary.totalRescueTeams}
                    <span style={{ fontSize: '0.9rem', color: '#64748b', fontWeight: 500 }}>
                      {' '}({summary.deployedRescueTeams} deployed)
                    </span>
                  </div>
                  <div className="metric-label">Rescue Teams</div>
                </div>

                <div className="metric-card">
                  <span className="metric-icon">🔄</span>
                  <div className="metric-value">{summary.completedRedistributions}</div>
                  <div className="metric-label">Redistribution Transfers</div>
                </div>
              </div>

              {/* Rescue Teams Operational Roster */}
              <div className="card" style={{ padding: '1.25rem', marginTop: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h3 style={{ margin: 0, fontSize: '1.1rem' }}>🚒 Emergency Response Team Deployments</h3>
                  <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    <strong>{summary.deployedRescueTeams}</strong> deployed • <strong>{summary.availableRescueTeams}</strong> ready for dispatch
                  </span>
                </div>
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Team ID</th>
                        <th>Rescue Unit</th>
                        <th>Availability</th>
                        <th>Assigned Disaster Area</th>
                        <th>Operational Posture</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rescueTeams.map((team) => (
                        <tr key={team.teamId}>
                          <td><strong>#{team.teamId}</strong></td>
                          <td><strong>{team.teamName}</strong></td>
                          <td>
                            <span
                              className={`badge ${
                                team.availability === 'DEPLOYED'
                                  ? 'badge-warning'
                                  : team.availability === 'AVAILABLE'
                                  ? 'badge-success'
                                  : 'badge-secondary'
                              }`}
                            >
                              {team.availability}
                            </span>
                          </td>
                          <td>
                            {team.assignedRegionName && team.assignedRegionName !== 'Unassigned' ? (
                              <span>📍 {team.assignedRegionName}</span>
                            ) : (
                              <span style={{ color: '#94a3b8' }}>Staging Depot / Unassigned</span>
                            )}
                          </td>
                          <td>
                            {team.availability === 'DEPLOYED' ? (
                              <span style={{ color: '#b45309', fontWeight: 600 }}>Active in disaster sector</span>
                            ) : (
                              <span style={{ color: '#15803d', fontWeight: 600 }}>On standby for immediate mobilization</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: OPERATIONS VISUALIZATIONS */}
          {activeTab === 'charts' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              {/* Row 1: Stock Distribution & Severity */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
                {/* Chart 1: Available vs Reserved Stock */}
                <div className="card" style={{ padding: '1.25rem' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem' }}>
                    📦 Resource Stock by Disaster Sector
                  </h3>
                  <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                    Comparison of available operational stock vs. protected safety reserves.
                  </p>
                  <div style={{ height: '300px', width: '100%' }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={resourceDistributionData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                        <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                        <YAxis tick={{ fontSize: 11 }} />
                        <Tooltip />
                        <Legend wrapperStyle={{ fontSize: 12 }} />
                        <Bar dataKey="available" name="Available Stock" fill="#0284c7" />
                        <Bar dataKey="reserved" name="Reserved Buffer" fill="#f59e0b" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                {/* Chart 2: Severity vs Priority */}
                <div className="card" style={{ padding: '1.25rem' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem' }}>
                    ⚡ Disaster Severity &amp; Priority Ratings
                  </h3>
                  <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                    Severity scale (1–10) alongside calculated emergency priority score.
                  </p>
                  <div style={{ height: '300px', width: '100%' }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={severityPriorityData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                        <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                        <YAxis tick={{ fontSize: 11 }} />
                        <Tooltip />
                        <Legend wrapperStyle={{ fontSize: 12 }} />
                        <Bar dataKey="severity" name="Severity Level (1-10)" fill="#e11d48" />
                        <Bar dataKey="priority" name="Priority Score" fill="#6366f1" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>

              {/* Row 2: Request Status Breakdown & Redistribution Volume */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
                {/* Chart 3: Request Status Breakdown */}
                <div className="card" style={{ padding: '1.25rem' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem' }}>
                    📋 Relief Demand Fulfillment Status
                  </h3>
                  <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                    Proportion of demands fulfilled, partially satisfied, and currently pending.
                  </p>
                  <div style={{ height: '300px', width: '100%' }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={requestStatusData}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          outerRadius={95}
                          label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                        >
                          {requestStatusData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip />
                        <Legend wrapperStyle={{ fontSize: 12 }} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                {/* Chart 4: Redistribution Volume by Resource */}
                <div className="card" style={{ padding: '1.25rem' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem' }}>
                    🔄 Units Moved in Completed Redistributions
                  </h3>
                  <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                    Total supplies successfully transferred across sectors to eliminate shortages.
                  </p>
                  <div style={{ height: '300px', width: '100%' }}>
                    {redistributionVolumeData.length === 0 ? (
                      <div className="empty-state" style={{ height: '100%' }}>
                        <p>No historical redistribution units recorded yet.</p>
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={redistributionVolumeData}>
                          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                          <XAxis dataKey="resource" tick={{ fontSize: 11 }} />
                          <YAxis tick={{ fontSize: 11 }} />
                          <Tooltip />
                          <Bar dataKey="units" name="Units Transferred" fill="#0d9488" />
                        </BarChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                </div>
              </div>

              {/* Row 3: Dispatched Units by Recipient Sector */}
              <div className="card" style={{ padding: '1.25rem' }}>
                <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem' }}>
                  🚚 Allocation Volume Dispatched to Affected Sectors
                </h3>
                <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                  Total units dispatched across completed allocation operations.
                </p>
                <div style={{ height: '280px', width: '100%' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={allocationLogisticsData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                      <Bar dataKey="units" name="Units Dispatched" fill="#3b82f6" />
                      <Bar dataKey="dispatches" name="Dispatches Count" fill="#8b5cf6" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: REGIONAL IMPACT & COVERAGE */}
          {activeTab === 'regions' && (
            <div className="card" style={{ padding: '1.25rem' }}>
              <div style={{ marginBottom: '1rem' }}>
                <h3 style={{ margin: 0, fontSize: '1.15rem' }}>Sector-Wise Crisis &amp; Relief Summary</h3>
                <p style={{ margin: '0.25rem 0 0', fontSize: '0.88rem', color: '#64748b' }}>
                  Consolidated view of disaster severity, casualty counts, available inventory, unmet deficits, and assigned response units.
                </p>
              </div>

              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Sector</th>
                      <th>Location</th>
                      <th style={{ textAlign: 'center' }}>Severity</th>
                      <th style={{ textAlign: 'right' }}>Affected Pop.</th>
                      <th style={{ textAlign: 'right' }}>Available Stock</th>
                      <th style={{ textAlign: 'right' }}>Pending Deficit</th>
                      <th style={{ textAlign: 'center' }}>Fulfillment %</th>
                      <th>Assigned Response Teams</th>
                    </tr>
                  </thead>
                  <tbody>
                    {regionAnalytics.map((reg) => (
                      <tr key={reg.regionId}>
                        <td>
                          <strong>{reg.regionName}</strong>
                          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                            Priority: <strong>{reg.priorityScore?.toFixed(2)}</strong>
                          </div>
                        </td>
                        <td>{reg.location}</td>
                        <td style={{ textAlign: 'center' }}>
                          <span
                            className={`badge ${
                              reg.disasterSeverity >= 8
                                ? 'badge-danger'
                                : reg.disasterSeverity >= 5
                                ? 'badge-warning'
                                : 'badge-success'
                            }`}
                          >
                            Level {reg.disasterSeverity}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <strong>{reg.populationAffected?.toLocaleString()}</strong>
                          <div style={{ fontSize: '0.75rem', color: '#dc2626' }}>
                            {reg.injuredPeople} casualties
                          </div>
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 600 }}>
                          {reg.totalAvailableStock?.toLocaleString()} units
                          <div style={{ fontSize: '0.75rem', color: '#b45309' }}>
                            {reg.totalReservedStock} reserved
                          </div>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <strong style={{ color: reg.pendingRequiredUnits > 0 ? '#dc2626' : '#15803d' }}>
                            {reg.pendingRequiredUnits?.toLocaleString()} units
                          </strong>
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>
                            {reg.fulfillmentPercentage}%
                          </div>
                          <div
                            style={{
                              width: '80px',
                              height: '6px',
                              background: '#e2e8f0',
                              borderRadius: '3px',
                              margin: '0.2rem auto 0',
                              overflow: 'hidden'
                            }}
                          >
                            <div
                              style={{
                                width: `${Math.min(100, reg.fulfillmentPercentage)}%`,
                                height: '100%',
                                background: reg.fulfillmentPercentage === 100 ? '#10b981' : '#f59e0b'
                              }}
                            />
                          </div>
                        </td>
                        <td>
                          {reg.assignedTeams && reg.assignedTeams.length > 0 ? (
                            reg.assignedTeams.map((t, idx) => (
                              <span key={idx} className="badge badge-primary" style={{ fontSize: '0.75rem', marginRight: '0.25rem' }}>
                                🚒 {t}
                              </span>
                            ))
                          ) : (
                            <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>None Assigned</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 4: OPERATIONAL REPORTS & EXPORTS */}
          {activeTab === 'reports' && (
            <div>
              <div className="card" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
                <h3 style={{ margin: '0 0 0.5rem 0' }}>📥 Emergency Operational Reports Export</h3>
                <p style={{ margin: '0 0 1.25rem 0', color: '#64748b', fontSize: '0.9rem' }}>
                  Download official CSV reports containing real-time records directly from the operations registry.
                </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
                  {/* Export 1: Regions */}
                  <div className="card" style={{ padding: '1.25rem', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.35rem 0' }}>📍 Disaster Sectors Report</h4>
                    <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                      Severity ratings, casualties, population metrics, priority scores, and team deployments.
                    </p>
                    <button onClick={exportRegionReport} className="btn btn-outline-dark btn-sm" style={{ width: '100%' }}>
                      📥 Download Sectors CSV ({regionAnalytics.length} rows)
                    </button>
                  </div>

                  {/* Export 2: Inventory */}
                  <div className="card" style={{ padding: '1.25rem', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.35rem 0' }}>📦 Supply Inventory Report</h4>
                    <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                      Warehouse stock, available items, safety reserve buffers, and redistributable surplus.
                    </p>
                    <button onClick={exportInventoryReport} className="btn btn-outline-dark btn-sm" style={{ width: '100%' }}>
                      📥 Download Inventory CSV ({resources.length} rows)
                    </button>
                  </div>

                  {/* Export 3: Demands */}
                  <div className="card" style={{ padding: '1.25rem', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.35rem 0' }}>📋 Relief Demands Report</h4>
                    <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                      Active queue items, required quantities, allocated units, and fulfillment status.
                    </p>
                    <button onClick={exportDemandsReport} className="btn btn-outline-dark btn-sm" style={{ width: '100%' }}>
                      📥 Download Demands CSV ({requests.length} rows)
                    </button>
                  </div>

                  {/* Export 4: Allocations */}
                  <div className="card" style={{ padding: '1.25rem', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.35rem 0' }}>📑 Allocation Log Report</h4>
                    <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                      Dispatched supplies, supply depots, transit distances, and authorization officers.
                    </p>
                    <button onClick={exportAllocationReport} className="btn btn-outline-dark btn-sm" style={{ width: '100%' }}>
                      📥 Download Allocations CSV ({allocations.length} rows)
                    </button>
                  </div>

                  {/* Export 5: Redistributions */}
                  <div className="card" style={{ padding: '1.25rem', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.35rem 0' }}>🔄 Redistribution History Report</h4>
                    <p style={{ margin: '0 0 1rem 0', fontSize: '0.85rem', color: '#64748b' }}>
                      Historical surplus transfers, donor depots, recipient sectors, and transit durations.
                    </p>
                    <button onClick={exportRedistributionReport} className="btn btn-outline-dark btn-sm" style={{ width: '100%' }}>
                      📥 Download Redistribution CSV ({redistributions.length} rows)
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
