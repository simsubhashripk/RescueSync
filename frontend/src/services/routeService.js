import { getAuthHeaders } from './authService';

const BASE_URL = '/api/routes';

/**
 * Service for Phase 8 Dijkstra Shortest Route calculation APIs.
 */

export async function fetchShortestRoute(sourceRegionId, destinationRegionId) {
  const url = `${BASE_URL}/shortest?sourceRegionId=${sourceRegionId}&destinationRegionId=${destinationRegionId}`;
  const response = await fetch(url, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Failed to calculate shortest route.');
  }
  return data;
}

