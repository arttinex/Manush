/**
 * auth.js — Token storage, route guards, and auth state helpers.
 */
import { apiFetch } from './api.js';

export function getAccessToken() {
  return localStorage.getItem('access_token');
}

export function getRefreshToken() {
  return localStorage.getItem('refresh_token');
}

export function decodeJwtPayload(token) {
  if (!token) return null;
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }
    const decoded = atob(base64);
    try {
      return JSON.parse(
        decodeURIComponent(
          decoded
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        )
      );
    } catch {
      return JSON.parse(decoded);
    }
  } catch (e) {
    return null;
  }
}

export function isLoggedIn() {
  const token = getAccessToken();
  if (!token) return false;
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) {
    return token.split('.').length === 3;
  }
  return payload.exp * 1000 > Date.now();
}

export function getCurrentUserId() {
  const storedId = localStorage.getItem('user_id');
  if (storedId) return storedId;
  const token = getAccessToken();
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  return payload ? (payload.sub || payload.userId) : null;
}

export function getUserRoles() {
  const storedRoles = localStorage.getItem('user_roles');
  if (storedRoles) {
    try {
      const parsed = JSON.parse(storedRoles);
      if (Array.isArray(parsed)) return parsed;
    } catch {}
  }
  const token = getAccessToken();
  if (!token) return [];
  const payload = decodeJwtPayload(token);
  return (payload && payload.roles) || [];
}

export function isAdmin() {
  if (localStorage.getItem('is_admin') === 'true') return true;
  const roles = getUserRoles();
  return Array.isArray(roles) && roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN' || r?.name === 'ADMIN');
}

export function requireAuth(redirectUrl = '/auth/login') {
  if (!isLoggedIn()) {
    window.location.href = redirectUrl;
    return false;
  }
  return true;
}

export async function requireAdmin() {
  if (!isLoggedIn()) {
    window.location.href = '/auth/login';
    return;
  }
  if (!isAdmin()) {
    try {
      const user = await apiFetch('/users/me');
      const hasAdmin = (user.roles || []).some(r => r === 'ADMIN' || r === 'ROLE_ADMIN' || r?.name === 'ADMIN');
      if (!hasAdmin) {
        window.location.href = '/feed';
      } else {
        localStorage.setItem('is_admin', 'true');
      }
    } catch {
      window.location.href = '/auth/login';
    }
  }
}

export function logout() {
  localStorage.clear();
  window.location.href = '/auth/login';
}

function renderNavAuth(container, userName, isUserAdmin) {
  let adminBadge = '';
  if (isUserAdmin) {
    adminBadge = `<a href="/admin/moderation" class="btn btn-ghost" style="color:var(--manush-warning);font-weight:700;display:inline-flex;align-items:center;gap:4px">🛡️ Admin</a>`;
  }

  container.innerHTML = `
    ${adminBadge}
    <a href="/profile" class="btn btn-ghost" style="display:inline-flex;align-items:center;gap:6px">
      <span>👤</span> <span>${userName}</span>
    </a>
    <button id="logoutBtn" class="btn btn-ghost">Logout</button>
  `;

  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      localStorage.clear();
      window.location.href = '/auth/login';
    });
  }
}

export async function setupNavbar() {
  const authArea = document.getElementById('authArea');
  if (!authArea) return;

  if (!isLoggedIn()) {
    authArea.innerHTML = `
      <a href="/auth/login" class="btn btn-ghost">Login</a>
      <a href="/auth/register" class="btn btn-3d">Join</a>
    `;
    return;
  }

  let userName = localStorage.getItem('user_name') || 'Profile';
  let isUserAdmin = isAdmin();

  renderNavAuth(authArea, userName, isUserAdmin);

  try {
    const user = await apiFetch('/users/me');
    if (user && user.name) {
      userName = user.name.split(' ')[0];
      localStorage.setItem('user_name', userName);
    }
    const hasAdmin = (user?.roles || []).some(r => r === 'ADMIN' || r === 'ROLE_ADMIN' || r?.name === 'ADMIN');
    if (hasAdmin) {
      isUserAdmin = true;
      localStorage.setItem('is_admin', 'true');
    }
    renderNavAuth(authArea, userName, isUserAdmin);
  } catch (e) {
    // already rendered fallback
  }
}
