/**
 * theme.js — Centralized theme management for Manush.
 */
export function initTheme() {
  const current = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', current);

  const toggleBtn = document.getElementById('themeToggle');
  if (toggleBtn) {
    toggleBtn.textContent = current === 'dark' ? '☀️' : '🌙';
    toggleBtn.onclick = () => {
      const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
      const next = isDark ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
      toggleBtn.textContent = next === 'dark' ? '☀️' : '🌙';
    };
  }
}

// Automatically initialize if imported or loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initTheme);
} else {
  initTheme();
}
