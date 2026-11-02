import { getAuthHeaders } from './authService';

const BASE_URL = '/api/regions';

/**
 * Fetch all disaster-affected regions.
 */
export async function fetchAllRegions() {
  const response = await fetch(BASE_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve disaster areas.');
  }
  return data;
}

/**
 * Fetch a single disaster-affected region by ID.
 */
export async function fetchRegionById(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve region ID ${id}.`);
  }
  return data;
}

/**
 * Register a new disaster-affected region (Admin only).
 */
export async function createRegion(payload) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to register disaster area.');
  }
  return data;
}

/**
 * Update an existing disaster-affected region (Admin only).
 */
export async function updateRegion(id, payload) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to update region ID ${id}.`);
  }
  return data;
}

/**
 * Delete a disaster-affected region (Admin only).
 * If dependencies exist, backend returns HTTP 409 Conflict with explanation.
 */
export async function deleteRegion(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to delete region ID ${id}.`);
  }
  return data;
}

/**
 * Frontend priority score estimator matching the desktop algorithm.
 */
export function estimatePriorityScore({
  populationAffected = 0,
  disasterSeverity = 1,
  injuredPeople = 0,
  vulnerablePeople = 0,
  roadAccessibility = 1.0,
}) {
  const pop = Math.max(0, Number(populationAffected) || 0);
  const sev = Math.min(10, Math.max(1, Number(disasterSeverity) || 1));
  const injured = Math.max(0, Number(injuredPeople) || 0);
  const vulnerable = Math.max(0, Number(vulnerablePeople) || 0);
  const road = Math.min(1.0, Math.max(0.0, Number(roadAccessibility) || 0.0));

  const severityComponent = (sev / 10.0) * 35.0;
  const injuryRatio = pop > 0 ? Math.min(1.0, injured / pop) : 0.0;
  const injuryComponent = injuryRatio * 25.0;

  const vulnerableRatio = pop > 0 ? Math.min(1.0, vulnerable / pop) : 0.0;
  const vulnerableComponent = vulnerableRatio * 20.0;

  const popFactor = Math.min(1.0, Math.log10(Math.max(10, pop)) / 4.0);
  const popComponent = popFactor * 10.0;

  const accessibilityUrgencyComponent = (1.0 - road) * 10.0;

  const total = severityComponent + injuryComponent + vulnerableComponent + popComponent + accessibilityUrgencyComponent;
  return Math.round(total * 100) / 100;
}

