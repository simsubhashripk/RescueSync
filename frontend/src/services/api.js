/**
 * RescueSync API client service.
 */
export async function checkBackendHealth() {
  try {
    const response = await fetch('/api/health');
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Failed to connect to backend /api/health:', error);
    return { status: 'DOWN', error: error.message };
  }
}

