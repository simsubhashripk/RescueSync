import { getAuthHeaders } from './authService';

const BASE_URL = '/api/redistribution';

/**
 * Service for Phase 9 Resource Shortage/Surplus Detection and Redistribution APIs.
 */

export async function fetchShortages() {
  const response = await fetch(`${BASE_URL}/shortages`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve resource shortages.');
  }
  return data;
}

export async function fetchSurpluses() {
  const response = await fetch(`${BASE_URL}/surpluses`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve resource surpluses.');
  }
  return data;
}

export async function fetchRecommendations() {
  const response = await fetch(`${BASE_URL}/recommendations`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve redistribution recommendations.');
  }
  return data;
}

export async function generateRecommendations() {
  const response = await fetch(`${BASE_URL}/recommendations/generate`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to generate recommendations.');
  }
  return data;
}

export async function fetchRedistributionHistory() {
  const response = await fetch(`${BASE_URL}/history`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve redistribution history.');
  }
  return data;
}

export async function fetchRedistributionSummary() {
  const response = await fetch(`${BASE_URL}/summary`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve redistribution summary.');
  }
  return data;
}

export async function executeTransfer(recordId) {
  const response = await fetch(`${BASE_URL}/execute/${recordId}`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `Failed to execute redistribution transfer #${recordId}.`);
  }
  return data;
}

export async function executeBatchRedistribution() {
  const response = await fetch(`${BASE_URL}/execute-batch`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to execute batch redistribution.');
  }
  return data;
}

