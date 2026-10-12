/**
 * toast.js — SweetAlert2 Toast mixin wrapper.
 */

const Toast = (typeof Swal !== 'undefined') ? Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.addEventListener('mouseenter', Swal.stopTimer);
    toast.addEventListener('mouseleave', Swal.resumeTimer);
  }
}) : null;

export function showSuccess(message) {
  if (Toast) Toast.fire({ icon: 'success', title: message });
  else console.log('[Toast Success]', message);
}

export function showError(message) {
  if (Toast) Toast.fire({ icon: 'error', title: message });
  else console.error('[Toast Error]', message);
}

export function showWarning(message) {
  if (Toast) Toast.fire({ icon: 'warning', title: message });
  else console.warn('[Toast Warning]', message);
}

export function showInfo(message) {
  if (Toast) Toast.fire({ icon: 'info', title: message });
  else console.info('[Toast Info]', message);
}

export { Toast };
