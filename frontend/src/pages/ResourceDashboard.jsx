import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchAllResources, deleteResource } from '../services/resourceService';
import { fetchAllRegions } from '../services/regionService';

export default function ResourceDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [resources, setResources] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRegionId, setSelectedRegionId] = useState('ALL');
  const [stockStatusFilter, setStockStatusFilter] = useState('ALL');
  const [deletingId, setDeletingId] = useState(null);

  const isAdmin = user && (user.role === 'ADMIN' || user.role === 'ADMINISTRATOR');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [resData, regData] = await Promise.all([
        fetchAllResources(),
        fetchAllRegions(),
      ]);
      setResources(resData);
      setRegions(regData);
    } catch (err) {
      setError(err.message || 'Failed to load resource inventory data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDelete = async (id, name, regionName) => {
    if (!window.confirm(`Are you sure you want to delete "${name}" from ${regionName}? Associated relief operations may prevent deletion.`)) {
      return;
    }

    setDeletingId(id);
    setError('');
    setSuccessMsg('');

    try {
      const res = await deleteResource(id);
      setSuccessMsg(res.message || `Resource #${id} deleted successfully.`);
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  };

  // Metrics summary
  const totalRecords = resources.length;
  const totalAvailable = resources.reduce((acc, r) => acc + (r.availableQuantity || 0), 0);
  const totalReserved = resources.reduce((acc, r) => acc + (r.reservedQuantity || 0), 0);
  const totalSurplus = resources.reduce((acc, r) => acc + (r.surplusQuantity || 0), 0);

  // Category counts
  const categoryCounts = resources.reduce((acc, r) => {
    const key = r.resourceName || 'Other';
    acc[key] = (acc[key] || 0) + (r.availableQuantity || 0);
    return acc;
  }, {});

  const getStockStatus = (r) => {
    if (r.availableQuantity === 0) return 'DEPLETED';
    if (r.availableQuantity < r.reservedQuantity) return 'CRITICAL';
    if (r.availableQuantity === r.reservedQuantity) return 'AT_BUFFER';
    return 'SURPLUS';
  };

  const filteredResources = resources.filter((r) => {
    // Search query
    const q = searchQuery.toLowerCase().trim();
    if (q) {
      const matchName = r.resourceName?.toLowerCase().includes(q);
      const matchRegion = r.regionName?.toLowerCase().includes(q);
      if (!matchName && !matchRegion) return false;
    }

    // Region filter
    if (selectedRegionId !== 'ALL' && String(r.regionId) !== String(selectedRegionId)) {
      return false;
    }

    // Stock status filter
    const status = getStockStatus(r);
    if (stockStatusFilter === 'DEPLETED' && status !== 'DEPLETED') return false;
    if (stockStatusFilter === 'CRITICAL' && status !== 'CRITICAL') return false;
    if (stockStatusFilter === 'SURPLUS' && status !== 'SURPLUS') return false;
    if (stockStatusFilter === 'LOW_STOCK' && status !== 'DEPLETED' && status !== 'CRITICAL') return false;

    return true;
  });

  const getStatusBadge = (r) => {
    const status = getStockStatus(r);
    if (status === 'DEPLETED') {
      return <span className="badge badge-stock-depleted">⚠️ Depleted (0)</span>;
    }
    if (status === 'CRITICAL') {
      return <span className="badge badge-stock-critical">⚡ Below Buffer</span>;
    }
    if (status === 'AT_BUFFER') {
      return <span className="badge badge-stock-buffer">🔒 At Buffer ({r.reservedQuantity})</span>;
    }
    return <span className="badge badge-stock-surplus">✓ Surplus (+{r.surplusQuantity})</span>;
  };

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Dashboard</Link> / <span>Resource Inventory</span>
          </div>
          <h1 className="page-title">Disaster Resource Inventory</h1>
          <p className="page-desc">
            Monitor regional logistics warehouses, emergency stockpile buffers, and surplus relief supplies available for resource redistribution.
          </p>
        </div>
        {isAdmin && (
          <div>
            <Link to="/resources/new" className="btn btn-primary">
              <span style={{ marginRight: '0.4rem' }}>📦</span> Add Inventory Item
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

      {/* Resource Inventory Dashboard Spotlight */}
      <div className="metric-spotlight-grid">
        <div className="spotlight-card">
          <span className="spotlight-label">Total Inventory Items</span>
          <div className="spotlight-value">{totalRecords}</div>
          <p className="spotlight-caption">Distinct supply categories across all registered disaster regions.</p>
        </div>

        <div className="spotlight-card">
          <span className="spotlight-label">Total Units Available</span>
          <div className="spotlight-value" style={{ color: '#0f766e' }}>
            {totalAvailable.toLocaleString()}
          </div>
          <p className="spotlight-caption">Aggregate physical units currently stored in regional depots.</p>
        </div>

        <div className="spotlight-card">
          <span className="spotlight-label">Reserved Emergency Buffers</span>
          <div className="spotlight-value" style={{ color: '#b45309' }}>
            {totalReserved.toLocaleString()}
          </div>
          <p className="spotlight-caption">Protected threshold quantities reserved for localized sector crises.</p>
        </div>

        <div className="spotlight-card priority-spotlight">
          <span className="spotlight-label">Net Surplus for Redistribution</span>
          <div className="spotlight-value" style={{ color: '#dc2626' }}>
            {totalSurplus.toLocaleString()}
          </div>
          <p className="spotlight-caption">Mobile units exceeding local reserve limits eligible for inter-sector transfer.</p>
        </div>
      </div>

      {/* Category Summary Pills */}
      <div className="category-summary-container">
        <span className="category-summary-title">Regional Stock Totals by Category:</span>
        <div className="category-pills-list">
          {Object.entries(categoryCounts).map(([cat, count]) => (
            <span key={cat} className="category-pill">
              <strong>{cat}:</strong> {count.toLocaleString()} units
            </span>
          ))}
        </div>
      </div>

      {/* Filters and Search Bar */}
      <div className="filter-bar">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Search by resource or sector name..."
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
            value={stockStatusFilter}
            onChange={(e) => setStockStatusFilter(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Stock Levels</option>
            <option value="SURPLUS">Has Redistributable Surplus</option>
            <option value="LOW_STOCK">Low Stock / Critical</option>
            <option value="CRITICAL">Below Emergency Buffer</option>
            <option value="DEPLETED">Completely Depleted (0)</option>
          </select>
        </div>

        <div className="record-count">
          Showing <strong>{filteredResources.length}</strong> of {resources.length} items
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading resource inventory catalog...</p>
        </div>
      ) : filteredResources.length === 0 ? (
        <div className="empty-state-card">
          <div className="empty-icon">📦</div>
          <h3>No Matching Inventory Records</h3>
          <p>
            {searchQuery || selectedRegionId !== 'ALL' || stockStatusFilter !== 'ALL'
              ? 'No resources match your active filter criteria. Try adjusting your search keywords or region selection.'
              : 'There are currently no resource items registered in the database.'}
          </p>
          {isAdmin && (
            <Link to="/resources/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>
              Add First Resource Item
            </Link>
          )}
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '60px' }}>ID</th>
                <th>Resource Name</th>
                <th>Assigned Sector</th>
                <th style={{ textAlign: 'right' }}>Available Stock</th>
                <th style={{ textAlign: 'right' }}>Reserved Buffer</th>
                <th style={{ textAlign: 'right' }}>Redistributable Surplus</th>
                <th style={{ textAlign: 'center' }}>Stock Health</th>
                <th style={{ textAlign: 'center' }}>Last Updated</th>
                {isAdmin && <th style={{ textAlign: 'center', width: '130px' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredResources.map((item) => (
                <tr key={item.resourceId} className="table-row">
                  <td className="row-id">#{item.resourceId}</td>
                  <td>
                    <strong>{item.resourceName}</strong>
                  </td>
                  <td>
                    <Link to={`/regions/${item.regionId}`} className="region-name-link">
                      {item.regionName}
                    </Link>
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 700 }}>
                    <span style={{ color: item.availableQuantity === 0 ? '#dc2626' : '#0f172a' }}>
                      {item.availableQuantity?.toLocaleString()}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right', color: '#64748b' }}>
                    {item.reservedQuantity?.toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 700 }}>
                    <span style={{ color: item.surplusQuantity > 0 ? '#16a34a' : '#94a3b8' }}>
                      {item.surplusQuantity > 0 ? `+${item.surplusQuantity?.toLocaleString()}` : '0'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {getStatusBadge(item)}
                  </td>
                  <td style={{ textAlign: 'center', fontSize: '0.8rem', color: '#64748b' }}>
                    {item.updatedAt ? new Date(item.updatedAt).toLocaleDateString() : 'Baseline'}
                  </td>
                  {isAdmin && (
                    <td style={{ textAlign: 'center' }}>
                      <div className="table-actions">
                        <button
                          className="btn-action btn-action-edit"
                          onClick={() => navigate(`/resources/${item.resourceId}/edit`)}
                          title="Edit Resource Stock"
                        >
                          Edit
                        </button>
                        <button
                          className="btn-action btn-action-delete"
                          onClick={() => handleDelete(item.resourceId, item.resourceName, item.regionName)}
                          disabled={deletingId === item.resourceId}
                          title="Safely Delete Resource"
                        >
                          {deletingId === item.resourceId ? '...' : 'Del'}
                        </button>
                      </div>
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

