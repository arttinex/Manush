/**
 * api.js — Central fetch wrapper for Manush.
 * Every API call goes through apiFetch() so headers, auth, and errors are consistent.
 */

const API_BASE = '/api/v1';

function getAccessToken() {
  return localStorage.getItem('access_token');
}

function getRefreshToken() {
  return localStorage.getItem('refresh_token');
}

async function tryRefresh() {
  const rt = getRefreshToken();
  if (!rt) {
    window.location.href = '/auth/login';
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt })
    });
    if (!res.ok) throw new Error('Refresh failed');
    const data = await res.json();
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);
  } catch {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/auth/login';
  }
}

export async function apiFetch(path, options = {}, _retried = false) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  };
  const token = getAccessToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401 && !_retried) {
    await tryRefresh();
    return apiFetch(path, options, true);
  }

  if (!res.ok) {
    let body = {};
    try { body = await res.json(); } catch {}
    if (typeof Swal !== 'undefined') {
      Swal.fire({
        icon: 'error',
        title: 'Something went wrong',
        text: body.message || 'Please try again.',
        confirmButtonColor: 'var(--manush-primary)'
      });
    }
    throw new Error(body.message || `HTTP ${res.status}`);
  }

  return res.status === 204 ? null : res.json();
}

export { getAccessToken, getRefreshToken };
