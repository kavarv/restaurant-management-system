/**
 * main.js — Global JS loaded on every page (except KDS / login).
 * Handles: topbar dropdown, sidebar mobile toggle, modal helpers,
 *          CSRF utility, WebSocket connection for notifications.
 */
(function () {
  'use strict';

  /* ── CSRF helpers ─────────────────────────────────────────────────── */
  function getCsrfToken()  { var m = document.querySelector('meta[name="_csrf"]');        return m ? m.content : ''; }
  function getCsrfHeader() { var m = document.querySelector('meta[name="_csrf_header"]'); return m ? m.content : 'X-CSRF-TOKEN'; }

  /** Fetch wrapper that automatically adds the CSRF header */
  window.rmsApi = {
    get: function (url) {
      return fetch(url, { credentials: 'same-origin' });
    },
    post: function (url, body) {
      var headers = { 'Content-Type': 'application/json' };
      headers[getCsrfHeader()] = getCsrfToken();
      return fetch(url, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(body),
        credentials: 'same-origin'
      });
    },
    put: function (url, body) {
      var headers = { 'Content-Type': 'application/json' };
      headers[getCsrfHeader()] = getCsrfToken();
      return fetch(url, {
        method: 'PUT',
        headers: headers,
        body: JSON.stringify(body),
        credentials: 'same-origin'
      });
    }
  };

  /* ── Mobile sidebar toggle ─────────────────────────────────────────── */
  var sidebarToggle = document.getElementById('sidebarToggle');
  var sidebar       = document.getElementById('appSidebar');
  if (sidebarToggle && sidebar) {
    sidebarToggle.style.display = 'flex';
    sidebarToggle.addEventListener('click', function () {
      sidebar.classList.toggle('open');
    });
    // Close if clicking outside
    document.addEventListener('click', function (e) {
      if (sidebar.classList.contains('open') &&
          !sidebar.contains(e.target) &&
          !sidebarToggle.contains(e.target)) {
        sidebar.classList.remove('open');
      }
    });
  }

  /* ── User avatar dropdown ──────────────────────────────────────────── */
  var avatarBtn    = document.getElementById('userAvatarBtn');
  var userDropdown = document.getElementById('userDropdown');
  if (avatarBtn && userDropdown) {
    avatarBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      userDropdown.classList.toggle('open');
    });
    document.addEventListener('click', function () {
      userDropdown.classList.remove('open');
    });
  }

  /* ── Global modal helpers ──────────────────────────────────────────── */
  window.openModal = function (id) {
    var m = document.getElementById(id);
    if (m) m.classList.add('open');
  };

  window.closeModal = function (id) {
    var m = document.getElementById(id);
    if (m) m.classList.remove('open');
  };

  // Close modal on backdrop click
  document.querySelectorAll('.modal-backdrop').forEach(function (backdrop) {
    backdrop.addEventListener('click', function (e) {
      if (e.target === backdrop) backdrop.classList.remove('open');
    });
  });

  // Close modal on Escape key
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-backdrop.open').forEach(function (m) {
        m.classList.remove('open');
      });
    }
  });

  /* ── Auto-dismiss alerts ───────────────────────────────────────────── */
  setTimeout(function () {
    document.querySelectorAll('.alert').forEach(function (a) {
      a.style.transition = 'opacity .5s';
      a.style.opacity = '0';
      setTimeout(function () { a.remove(); }, 500);
    });
  }, 5000);

  /* ── WebSocket — notification bell ────────────────────────────────── */
  var bellDot = document.getElementById('notificationDot');
  var bell    = document.getElementById('notificationBell');

  function connectWebSocket() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    try {
      var socket = new SockJS('/ws');
      var stompClient = Stomp.over(socket);
      stompClient.debug = null; // suppress console noise

      stompClient.connect({}, function () {
        // Listen for new pending orders
        stompClient.subscribe('/topic/orders/new', function (msg) {
          if (bellDot) { bellDot.style.display = 'block'; }
          if (bell)    { bell.style.animation = 'none'; void bell.offsetWidth; bell.style.animation = 'bell-ring .4s 3'; }
        });
        // Listen for floor updates (handled by floor.js if present)
        stompClient.subscribe('/topic/floor', function (msg) {
          if (window.onFloorUpdate) window.onFloorUpdate(JSON.parse(msg.body));
        });
        // Waiter: order status updates
        stompClient.subscribe('/topic/orders/status', function (msg) {
          if (window.onOrderStatusUpdate) window.onOrderStatusUpdate(JSON.parse(msg.body));
        });
      }, function () {
        // Reconnect after 5 s on disconnect
        setTimeout(connectWebSocket, 5000);
      });
    } catch (err) {
      console.warn('WebSocket connection failed:', err);
    }
  }

  // Clear notification dot when bell is clicked
  if (bell) {
    bell.addEventListener('click', function () {
      if (bellDot) bellDot.style.display = 'none';
    });
  }

  connectWebSocket();

  /* ── Waiter dashboard: live order status badge updates ─────────────── */
  window.onOrderStatusUpdate = function (data) {
    var badge = document.querySelector('[data-order-id="' + data.orderId + '"]');
    if (badge) {
      badge.textContent = data.status;
      badge.className = 'badge badge-' + data.status;
    }
  };

})();
