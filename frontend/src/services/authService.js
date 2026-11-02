/**
 * RescueSync Authentication Service
 * Manages JWT tokens, API requests, and user sessions.
 */

const TOKEN_KEY = 'rescuesync_token';
const USER_KEY = 'rescuesync_user';

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

export function setSession(token, user) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getAuthHeaders() {
  const token = getStoredToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * Register a new public officer user.
 * Role is strictly enforced as USER by the backend.
 */
export async function registerUser({ fullName, username, email, password }) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: fullName.trim(),
      username: username.trim(),
      email: email.trim(),
      password,
    }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || data.error || 'Registration failed');
  }

  if (data.token && data.user) {
    setSession(data.token, data.user);
  }
  return data;
}

/**
 * Log in with existing credentials (PBKDF2 verified).
 */
export async function loginUser({ username, password }) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: username.trim(),
      password,
    }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || data.error || 'Login failed');
  }

  if (data.token && data.user) {
    setSession(data.token, data.user);
  }
  return data;
}

/**
 * Fetch authenticated profile using the Bearer token.
 */
export async function fetchCurrentUser() {
  const token = getStoredToken();
  if (!token) {
    return null;
  }

  const response = await fetch('/api/auth/me', {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    clearSession();
    return null;
  }

  const user = await response.json();
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  return user;
}

