import { getAuthHeaders } from './authService';

const BASE_URL = '/api/analytics';

/**
 * Service for fetching executive dashboard summaries, region-wise analytics,
 * and rescue team deployment records.
 */

export async function fetchAdminSummary() {
  const response = await fetch(`${BASE_URL}/summary`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve operations summary.');
  }
  return data;
}

export async function fetchRegionAnalytics() {
  const response = await fetch(`${BASE_URL}/regions`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve regional coverage analytics.');
  }
  return data;
}

export async function fetchRescueTeams() {
  const response = await fetch(`${BASE_URL}/teams`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to retrieve rescue teams data.');
  }
  return data;
}
