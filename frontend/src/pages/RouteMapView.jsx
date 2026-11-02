import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { fetchAllRegions } from '../services/regionService';
import { fetchShortestRoute } from '../services/routeService';

export default function RouteMapView() {
  const [regions, setRegions] = useState([]);
  const [loadingRegions, setLoadingRegions] = useState(true);
  const [error, setError] = useState(null);

  // Route selection states
  const [sourceRegionId, setSourceRegionId] = useState('');
  const [destRegionId, setDestRegionId] = useState('');
  const [calculatingRoute, setCalculatingRoute] = useState(false);
  const [calculatedRoute, setCalculatedRoute] = useState(null);

  // Map DOM & Leaflet instance refs
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const routeLayerRef = useRef(null);
  const markersLayerRef = useRef(null);

  // Load regions
  useEffect(() => {
    async function loadRegions() {
      try {
        setLoadingRegions(true);
        const data = await fetchAllRegions();
        setRegions(data);
        if (data.length >= 2) {
          setSourceRegionId(data[0].regionId.toString());
          setDestRegionId(data[data.length - 1].regionId.toString());
        }
      } catch (err) {
        console.error('Failed to load regions:', err);
        setError(err.message || 'Failed to load disaster sectors.');
      } finally {
        setLoadingRegions(false);
      }
    }
    loadRegions();
  }, []);

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    // Default center around Chennai/Tamil Nadu where baseline sectors reside (13.08, 80.27)
    const map = L.map(mapContainerRef.current, {
      center: [13.0827, 80.2707],
      zoom: 11,
      scrollWheelZoom: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
    }).addTo(map);

    markersLayerRef.current = L.layerGroup().addTo(map);
    routeLayerRef.current = L.layerGroup().addTo(map);

    mapInstanceRef.current = map;

    return () => {
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // Plot region markers whenever regions change
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !markersLayerRef.current || regions.length === 0) return;

    markersLayerRef.current.clearLayers();
    const latLngBounds = [];

    regions.forEach((region) => {
      const lat = region.latitude;
      const lng = region.longitude;

      if (lat == null || lng == null || isNaN(lat) || isNaN(lng)) return;

      latLngBounds.push([lat, lng]);

      // Severity color mapping
      const severity = region.disasterSeverity || 0;
      let markerColor = '#10b981'; // Green (Low)
      let severityLabel = 'Low';
      if (severity >= 8) {
        markerColor = '#e11d48'; // Red (Severe)
        severityLabel = 'Critical';
      } else if (severity >= 5) {
        markerColor = '#f59e0b'; // Amber (Moderate)
        severityLabel = 'Moderate';
      }

      // Custom HTML Marker Pin
      const icon = L.divIcon({
        className: 'custom-region-marker',
        html: `
          <div style="
            background-color: ${markerColor};
            width: 32px;
            height: 32px;
            border-radius: 50%;
            border: 3px solid #ffffff;
            box-shadow: 0 3px 8px rgba(0,0,0,0.35);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            font-weight: 800;
            font-size: 13px;
            cursor: pointer;
          ">
            ${region.regionId}
          </div>
        `,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const marker = L.marker([lat, lng], { icon });

      // Popup Content
      const popupHtml = `
        <div style="font-family: inherit; font-size: 13px; min-width: 180px; padding: 4px;">
          <h4 style="margin: 0 0 4px; color: #0f172a; font-size: 14px; font-weight: 700;">
            ${region.regionName}
          </h4>
          <div style="color: #64748b; font-size: 12px; margin-bottom: 6px;">
            Sector #${region.regionId} • Priority Score: <strong>${region.priorityScore != null ? region.priorityScore.toFixed(1) : '—'}</strong>
          </div>
          <div style="margin-bottom: 6px;">
            Severity: <span style="background: ${markerColor}20; color: ${markerColor}; padding: 2px 6px; border-radius: 4px; font-weight: 700;">
              Level ${severity} (${severityLabel})
            </span>
          </div>
          <div style="font-size: 11px; color: #475569; margin-bottom: 8px;">
            Population: <strong>${(region.populationAffected || 0).toLocaleString()}</strong><br/>
            Casualties: <strong>${region.injuredPeople || 0}</strong> • Vulnerable: <strong>${region.vulnerablePeople || 0}</strong>
          </div>
          <div style="display: flex; gap: 4px;">
            <button onclick="window.__selectSource(${region.regionId})" style="flex:1; font-size: 11px; padding: 3px 6px; background: #0f172a; color: white; border: none; border-radius: 3px; cursor: pointer;">
              Set Source
            </button>
            <button onclick="window.__selectDest(${region.regionId})" style="flex:1; font-size: 11px; padding: 3px 6px; background: #0284c7; color: white; border: none; border-radius: 3px; cursor: pointer;">
              Set Dest
            </button>
          </div>
        </div>
      `;

      marker.bindPopup(popupHtml);
      markersLayerRef.current.addLayer(marker);
    });

    if (latLngBounds.length > 0) {
      map.fitBounds(latLngBounds, { padding: [50, 50] });
    }

    // Expose helpers for popup buttons
    window.__selectSource = (id) => setSourceRegionId(id.toString());
    window.__selectDest = (id) => setDestRegionId(id.toString());

    return () => {
      delete window.__selectSource;
      delete window.__selectDest;
    };
  }, [regions]);

  // Handler: Calculate Dijkstra Route
  const handleCalculateRoute = async (srcId, dstId) => {
    const sId = srcId || sourceRegionId;
    const dId = dstId || destRegionId;

    if (!sId || !dId) {
      setError('Please select both a source depot and a destination disaster sector.');
      return;
    }

    try {
      setCalculatingRoute(true);
      setError(null);
      const data = await fetchShortestRoute(parseInt(sId, 10), parseInt(dId, 10));
      setCalculatedRoute(data);

      drawRouteOnMap(data);
    } catch (err) {
      console.error('Route calculation failed:', err);
      setError(err.message || 'Route calculation failed.');
      setCalculatedRoute(null);
      if (routeLayerRef.current) {
        routeLayerRef.current.clearLayers();
      }
    } finally {
      setCalculatingRoute(false);
    }
  };

  // Draw polyline and route markers on Leaflet
  const drawRouteOnMap = (routeData) => {
    const map = mapInstanceRef.current;
    if (!map || !routeLayerRef.current) return;

    routeLayerRef.current.clearLayers();

    if (!routeData.routeAvailable || !routeData.regionPath || routeData.regionPath.length < 2) {
      return;
    }

    // Map region IDs to coordinates
    const regionMap = new Map();
    regions.forEach((r) => regionMap.set(r.regionId, r));

    const pathCoordinates = [];
    routeData.regionPath.forEach((regId) => {
      const reg = regionMap.get(regId);
      if (reg && reg.latitude != null && reg.longitude != null) {
        pathCoordinates.push([reg.latitude, reg.longitude]);
      }
    });

    if (pathCoordinates.length < 2) return;

    // Draw route background line (glow effect)
    const glowPolyline = L.polyline(pathCoordinates, {
      color: '#0284c7',
      weight: 8,
      opacity: 0.35,
      lineCap: 'round',
      lineJoin: 'round',
    });

    // Draw main route polyline
    const routePolyline = L.polyline(pathCoordinates, {
      color: '#0284c7',
      weight: 4,
      opacity: 0.9,
      dashArray: '8, 8',
      lineCap: 'round',
      lineJoin: 'round',
    });

    const routeTooltip = `
      <div style="font-size: 12px; font-weight: 600;">
        ⚡ Fastest Relief Route<br/>
        ⏱️ Travel Time: <strong>${routeData.totalTravelTime} mins</strong><br/>
        🛣️ Distance: <strong>${routeData.totalDistanceKm} km</strong>
      </div>
    `;

    routePolyline.bindTooltip(routeTooltip, { sticky: true });

    routeLayerRef.current.addLayer(glowPolyline);
    routeLayerRef.current.addLayer(routePolyline);

    // Fit map to include both markers and the route
    map.fitBounds(routePolyline.getBounds(), { padding: [60, 60] });
  };

  const getRegionName = (id) => {
    const r = regions.find((reg) => reg.regionId === id);
    return r ? r.regionName : `Region #${id}`;
  };

  return (
    <div className="main-content">
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/">Home</Link> &gt; <Link to="/allocations">Allocations</Link> &gt; Relief Route Map
          </div>
          <h1 className="page-title">Relief Route Logistics Map</h1>
          <p className="page-subtitle">
            Interactive geospatial map identifying optimal transit corridors between supply depots and affected areas while avoiding blocked roads.
          </p>
        </div>
        <div className="page-actions-group">
          <Link to="/allocations" className="btn btn-outline-dark">
            📑 View Allocations
          </Link>
          <Link to="/requests/queue" className="btn btn-outline-dark">
            ⚡ Priority Queue
          </Link>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="alert alert-danger" role="alert">
          <strong>Notice:</strong> {error}
        </div>
      )}

      {/* Control & Query Bar */}
      <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
          <div style={{ flex: '1', minWidth: '220px' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              📍 Source Depot (Donor Region)
            </label>
            <select
              className="filter-select"
              style={{ width: '100%' }}
              value={sourceRegionId}
              onChange={(e) => setSourceRegionId(e.target.value)}
              disabled={loadingRegions}
            >
              {regions.map((r) => (
                <option key={r.regionId} value={r.regionId}>
                  #{r.regionId} - {r.regionName}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', paddingTop: '1.25rem' }}>
            <span style={{ fontSize: '1.5rem', color: '#64748b' }}>➔</span>
          </div>

          <div style={{ flex: '1', minWidth: '220px' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              🎯 Recipient Sector (Disaster Zone)
            </label>
            <select
              className="filter-select"
              style={{ width: '100%' }}
              value={destRegionId}
              onChange={(e) => setDestRegionId(e.target.value)}
              disabled={loadingRegions}
            >
              {regions.map((r) => (
                <option key={r.regionId} value={r.regionId}>
                  #{r.regionId} - {r.regionName}
                </option>
              ))}
            </select>
          </div>

          <div style={{ paddingTop: '1.25rem', display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={() => handleCalculateRoute()}
              className="btn btn-primary"
              disabled={calculatingRoute || loadingRegions || sourceRegionId === destRegionId}
            >
              {calculatingRoute ? (
                <span className="btn-loading-content">
                  <span className="mini-spinner" /> Routing...
                </span>
              ) : (
                '🧭 Find Optimal Route'
              )}
            </button>
          </div>
        </div>

        {/* Preset Fast-Test Buttons */}
        <div style={{ marginTop: '1rem', paddingTop: '0.85rem', borderTop: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
            Sample Operational Corridors:
          </span>
          <button
            onClick={() => {
              setSourceRegionId('1');
              setDestRegionId('4');
              handleCalculateRoute('1', '4');
            }}
            className="preset-pill"
          >
            Sector 1 ➔ Sector 4 (85m, 45km via 1➔2➔3➔4)
          </button>
          <button
            onClick={() => {
              setSourceRegionId('2');
              setDestRegionId('4');
              handleCalculateRoute('2', '4');
            }}
            className="preset-pill"
          >
            Sector 2 ➔ Sector 4 (Avoid Blocked 2➔4, via 2➔3➔4)
          </button>
          <button
            onClick={() => {
              setSourceRegionId('1');
              setDestRegionId('3');
              handleCalculateRoute('1', '3');
            }}
            className="preset-pill"
          >
            Sector 1 ➔ Sector 3 (via 1➔2➔3 @ 60m vs direct @ 95m)
          </button>
        </div>
      </div>

      {/* Calculated Route Detail Banner */}
      {calculatedRoute && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            padding: '1.25rem',
            borderLeft: calculatedRoute.routeAvailable ? '5px solid #0284c7' : '5px solid #e11d48',
            background: calculatedRoute.routeAvailable ? '#f0f9ff' : '#fff1f2',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
                <span
                  style={{
                    background: calculatedRoute.routeAvailable ? '#0284c7' : '#e11d48',
                    color: '#ffffff',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '0.2rem 0.5rem',
                    borderRadius: '4px',
                    textTransform: 'uppercase',
                  }}
                >
                  {calculatedRoute.routeAvailable ? 'Route Verified (OPEN)' : 'Corridor Blocked'}
                </span>
                <span style={{ fontSize: '0.9rem', color: '#0f172a', fontWeight: 600 }}>
                  {calculatedRoute.message}
                </span>
              </div>

              {calculatedRoute.routeAvailable && calculatedRoute.regionPath && (
                <div style={{ marginTop: '0.5rem', fontSize: '1rem', color: '#0f172a' }}>
                  Transit Corridor Path:{' '}
                  {calculatedRoute.regionPath.map((rId, idx) => (
                    <span key={rId}>
                      <strong style={{ color: '#0369a1' }}>{getRegionName(rId)}</strong>
                      {idx < calculatedRoute.regionPath.length - 1 && (
                        <span style={{ color: '#94a3b8', margin: '0 0.35rem' }}>➔</span>
                      )}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {calculatedRoute.routeAvailable && (
              <div style={{ display: 'flex', gap: '1.5rem', textAlign: 'right' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>
                    Est. Travel Time
                  </div>
                  <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0284c7' }}>
                    {calculatedRoute.totalTravelTime} mins
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>
                    Total Transit Distance
                  </div>
                  <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0f172a' }}>
                    {calculatedRoute.totalDistanceKm} km
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Map Container */}
      <div className="card" style={{ padding: '0.75rem', overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem 0.75rem' }}>
          <strong style={{ fontSize: '0.9rem', color: '#334155' }}>
            Regional Emergency Transit Network
          </strong>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
            Click markers to view regional casualty stats or set route endpoints
          </span>
        </div>
        <div
          ref={mapContainerRef}
          style={{
            height: '520px',
            width: '100%',
            borderRadius: '6px',
            boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.1)',
            zIndex: 1,
          }}
        />
      </div>
    </div>
  );
}

