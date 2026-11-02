import { getAuthHeaders } from './authService';

const BASE_URL = '/api/allocations';

/**
 * Service for Phase 7 Resource Allocation and Distribution APIs.
 */

export async function fetchAllAllocations() {
  const response = await fetch(BASE_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve allocation records.');
  }
  return data;
}

export async function fetchAllocationSummary() {
  const response = await fetch(`${BASE_URL}/summary`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve allocation summary.');
  }
  return data;
}

export async function fetchAllocationsByRequest(requestId) {
  const response = await fetch(`${BASE_URL}/request/${requestId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve allocations for request #${requestId}.`);
  }
  return data;
}

export async function fetchAllocationsByRegion(regionId) {
  const response = await fetch(`${BASE_URL}/region/${regionId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to retrieve allocations for region #${regionId}.`);
  }
  return data;
}

export async function allocateSingleRequest(requestId) {
  const response = await fetch(`${BASE_URL}/request/${requestId}`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to allocate resources for request #${requestId}.`);
  }
  return data;
}

export async function executeBatchAllocation() {
  const response = await fetch(`${BASE_URL}/execute`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to execute priority allocation batch.');
  }
  return data;
}
