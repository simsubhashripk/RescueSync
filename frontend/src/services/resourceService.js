import { getAuthHeaders } from './authService';

const BASE_URL = '/api/resources';

/**
 * Fetch all resource inventory records across all disaster sectors.
 */
export async function fetchAllResources() {
  const response = await fetch(BASE_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve resource inventory.');
  }
  return data;
}

/**
 * Fetch a specific resource item by ID.
 */
export async function fetchResourceById(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve resource item #${id}.`);
  }
  return data;
}

/**
 * Fetch all resources assigned to a particular disaster region.
 */
export async function fetchResourcesByRegion(regionId) {
  const response = await fetch(`${BASE_URL}/region/${regionId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve resources for region #${regionId}.`);
  }
  return data;
}

/**
 * Add a new resource item to a region (Admin only).
 */
export async function createResource(payload) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to register resource item.');
  }
  return data;
}

/**
 * Update an existing resource item (Admin only).
 */
export async function updateResource(id, payload) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to update resource item #${id}.`);
  }
  return data;
}

/**
 * Delete a resource item (Admin only, safety enforced).
 */
export async function deleteResource(id) {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to delete resource item #${id}.`);
  }
  return data;
}

