import { getAuthHeaders } from './authService';

const BASE_URL = '/api/requests';

/**
 * Fetch all relief requests across all sectors.
 */
export async function fetchAllRequests() {
  const response = await fetch(BASE_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve relief requests.');
  }
  return data;
}

/**
 * Fetch active PriorityQueue triage snapshot and summary statistics.
 */
export async function fetchPriorityQueueSummary() {
  const response = await fetch(`${BASE_URL}/queue`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve priority queue snapshot.');
  }
  return data;
}

/**
 * Fetch a specific relief request by ID.
 */
export async function fetchRequestById(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve request #${id}.`);
  }
  return data;
}

/**
 * Fetch all relief requests for a particular disaster region.
 */
export async function fetchRequestsByRegion(regionId) {
  const response = await fetch(`${BASE_URL}/region/${regionId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve requests for region #${regionId}.`);
  }
  return data;
}

/**
 * Submit a new relief demand for a disaster sector.
 */
export async function createReliefRequest(payload) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to create relief request.');
  }
  return data;
}

/**
 * Delete a relief request (Admin only, safety enforced).
 */
export async function deleteReliefRequest(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to delete relief request #${id}.`);
  }
  return data;
}

