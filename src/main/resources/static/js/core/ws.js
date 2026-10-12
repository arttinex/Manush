/**
 * ws.js — STOMP WebSocket singleton for Jogajog.
 * Usage: import { connectWs, subscribeMessages, sendMessage, disconnectWs } from '/static/js/core/ws.js';
 */

import { getAccessToken } from '/static/js/core/auth.js';

let stompClient = null;
let connected = false;
const subscriptions = [];

export function connectWs(onConnect, onDisconnect) {
  if (connected) { onConnect && onConnect(); return; }

  const token = getAccessToken();
  const wsUrl = '/ws';

  // Dynamically load SockJS + STOMP if not already loaded
  Promise.all([
    loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js'),
    loadScript('https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js')
  ]).then(() => {
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // silence STOMP debug logs

    const headers = token ? { Authorization: `Bearer ${token}` } : {};

    stompClient.connect(headers, () => {
      connected = true;
      console.log('[WS] Connected');
      onConnect && onConnect();
    }, (err) => {
      connected = false;
      console.warn('[WS] Disconnected or error', err);
      onDisconnect && onDisconnect();
    });
  });
}

export function subscribeMessages(destination, callback) {
  if (!stompClient || !connected) {
    console.warn('[WS] Not connected yet; subscription queued');
    subscriptions.push({ destination, callback });
    return;
  }
  stompClient.subscribe(destination, (frame) => {
    try { callback(JSON.parse(frame.body)); } catch (e) { callback(frame.body); }
  });
}

export function sendWsMessage(destination, body) {
  if (!stompClient || !connected) {
    console.warn('[WS] Cannot send — not connected');
    return;
  }
  stompClient.send(destination, {}, JSON.stringify(body));
}

export function disconnectWs() {
  if (stompClient) { stompClient.disconnect(); connected = false; }
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src='${src}']`)) { resolve(); return; }
    const s = document.createElement('script');
    s.src = src;
    s.onload = resolve;
    s.onerror = reject;
    document.head.appendChild(s);
  });
}
