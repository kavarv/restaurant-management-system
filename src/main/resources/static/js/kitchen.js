/**
 * kitchen.js — Kitchen Display System logic.
 * WebSocket connection, order card management, timers, audio alerts.
 * Loaded only on /kitchen (dashboard/chef.html).
 */
(function () {
  'use strict';

  var csrf       = document.querySelector('meta[name="_csrf"]')        ? document.querySelector('meta[name="_csrf"]').content        : '';
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]') ? document.querySelector('meta[name="_csrf_header"]').content : 'X-CSRF-TOKEN';

  /* ── Clock ─────────────────────────────────────────────────────────── */
  var kdsTime = document.getElementById('kdsTime');
  function updateClock() {
    if (kdsTime) kdsTime.textContent = new Date().toLocaleTimeString();
  }
  updateClock();
  setInterval(updateClock, 1000);

  /* ── Elapsed timers on each card ───────────────────────────────────── */
  function startTimers() {
    document.querySelectorAll('.kds-card').forEach(function (card) {
      var startAttr = card.getAttribute('data-start-time');
      var startMs   = startAttr ? new Date(startAttr).getTime() : Date.now();
      var timerEl   = card.querySelector('.kds-timer');
      if (!timerEl) return;
      timerEl.setAttribute('data-start', startMs);
    });
  }
  startTimers();

  setInterval(function () {
    document.querySelectorAll('.kds-timer[data-start]').forEach(function (el) {
      var start   = parseInt(el.getAttribute('data-start')) || Date.now();
      var elapsed = Math.floor((Date.now() - start) / 1000);
      var mins    = Math.floor(elapsed / 60);
      var secs    = elapsed % 60;
      el.textContent = pad(mins) + ':' + pad(secs);
      // Color thresholds
      el.className = 'kds-timer';
      if (elapsed > 600) el.classList.add('urgent');      // > 10 min
      else if (elapsed > 300) el.classList.add('warn');   // > 5 min
    });
  }, 1000);

  function pad(n) { return n < 10 ? '0' + n : '' + n; }

  /* ── Column item counts ────────────────────────────────────────────── */
  function updateCounts() {
    var colNew  = document.getElementById('colNew');
    var colPrep = document.getElementById('colPreparing');
    var colRdy  = document.getElementById('colReady');
    if (document.getElementById('newCount'))  document.getElementById('newCount').textContent  = colNew  ? colNew.querySelectorAll('.kds-card').length  : 0;
    if (document.getElementById('prepCount')) document.getElementById('prepCount').textContent = colPrep ? colPrep.querySelectorAll('.kds-card').length : 0;
    if (document.getElementById('readyCount'))document.getElementById('readyCount').textContent= colRdy  ? colRdy.querySelectorAll('.kds-card').length  : 0;
  }
  updateCounts();

  /* ── Status action buttons ─────────────────────────────────────────── */
  window.kdsAction = function (btn, newStatus) {
    var orderId = btn.getAttribute('data-order-id');
    btn.disabled = true;
    btn.textContent = 'Updating...';

    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrf;

    fetch('/api/v1/orders/' + orderId + '/status', {
      method: 'PUT',
      headers: headers,
      body: JSON.stringify({ status: newStatus }),
      credentials: 'same-origin'
    })
    .then(function (r) {
      if (!r.ok) throw new Error('Failed');
      return r.json();
    })
    .then(function () {
      moveCard(orderId, newStatus);
    })
    .catch(function () {
      btn.disabled = false;
      btn.textContent = 'Retry';
    });
  };

  function moveCard(orderId, status) {
    var card = document.querySelector('.kds-card[data-order-id="' + orderId + '"]');
    if (!card) return;

    card.classList.remove('new-order');

    if (status === 'PREPARING') {
      var colPrep = document.getElementById('colPreparing');
      if (colPrep) {
        // Remove placeholder text if present
        var placeholder = colPrep.querySelector('div:not(.kds-card)');
        if (placeholder) placeholder.remove();
        // Update button
        var btn = card.querySelector('button');
        if (btn) { btn.textContent = 'Mark Ready'; btn.className = 'btn btn-success btn-sm'; btn.style.width = '100%'; btn.setAttribute('onclick', 'kdsAction(this, "READY")'); btn.disabled = false; }
        colPrep.insertBefore(card, colPrep.firstChild);
        card.setAttribute('data-start-time', new Date().toISOString());
        startTimers();
      }
    } else if (status === 'READY') {
      var colReady = document.getElementById('colReady');
      if (colReady) {
        // Simplify the card for the ready column
        var btn = card.querySelector('button');
        if (btn) btn.remove();
        var oldBadge = card.querySelector('.badge');
        if (!oldBadge) {
          var badge = document.createElement('span');
          badge.className = 'badge badge-READY';
          badge.textContent = 'READY';
          card.querySelector('.kds-card-header').appendChild(badge);
        }
        colReady.insertBefore(card, colReady.firstChild);
      }
    }

    updateCounts();
  }

  /* ── WebSocket for new incoming orders ─────────────────────────────── */
  var wsStatus   = document.getElementById('wsStatus');
  var alertBadge = document.getElementById('newOrderAlert');

  function connectKDS() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
      if (wsStatus) wsStatus.textContent = 'WebSocket N/A (polling)';
      // Fallback: auto-refresh every 30 s
      setInterval(function () { location.reload(); }, 30000);
      return;
    }

    var socket = new SockJS('/ws');
    var stomp  = Stomp.over(socket);
    stomp.debug = null;

    stomp.connect({}, function () {
      if (wsStatus) { wsStatus.textContent = 'Live'; wsStatus.style.color = '#66BB6A'; }

      stomp.subscribe('/topic/kitchen', function (msg) {
        var order = JSON.parse(msg.body);
        if (order.status === 'CONFIRMED') {
          injectNewCard(order);
          flashAlert();
          notifyNewOrder(order);
        } else if (order.status === 'CANCELLED') {
          var card = document.querySelector('.kds-card[data-order-id="' + order.id + '"]');
          if (card) { card.style.opacity = '.3'; setTimeout(function () { card.remove(); updateCounts(); }, 1500); }
        }
      });
    }, function () {
      if (wsStatus) { wsStatus.textContent = 'Reconnecting...'; wsStatus.style.color = '#EF9A9A'; }
      setTimeout(connectKDS, 5000);
    });
  }

  function injectNewCard(order) {
    var colNew = document.getElementById('colNew');
    if (!colNew) return;
    var placeholder = colNew.querySelector('div:not(.kds-card)');
    if (placeholder) placeholder.remove();

    var itemsHtml = (order.items || []).map(function (item) {
      return '<div class="kds-item">'
        + '<span class="kds-item-qty">' + item.quantity + 'x</span>'
        + '<span>' + item.menuItemName + '</span>'
        + '</div>';
    }).join('');

    var card = document.createElement('div');
    card.className = 'kds-card new-order';
    card.setAttribute('data-order-id', order.id);
    card.setAttribute('data-start-time', order.createdAt || new Date().toISOString());
    card.innerHTML =
      '<div class="kds-card-header">'
      + '<span class="kds-order-num">#' + order.orderNumber + '</span>'
      + '<span class="kds-timer" data-start="' + Date.now() + '">00:00</span>'
      + '</div>'
      + '<div style="font-size:var(--fs-xs);color:#757575;margin-bottom:8px;">'
      + (order.tableNumber ? 'Table ' + order.tableNumber : 'Takeaway')
      + '</div>'
      + '<div class="kds-items">' + itemsHtml + '</div>'
      + '<button class="btn btn-warning btn-sm" style="margin-top:12px;width:100%;" '
      + 'data-order-id="' + order.id + '" onclick="kdsAction(this, \'PREPARING\')">Start Preparing</button>';

    colNew.insertBefore(card, colNew.firstChild);
    updateCounts();
    startTimers();
  }

  function flashAlert() {
    if (!alertBadge) return;
    alertBadge.style.display = 'inline-flex';
    setTimeout(function () { alertBadge.style.display = 'none'; }, 5000);
  }

  function notifyNewOrder(order) {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('New Order!', {
        body: 'Order #' + order.orderNumber + (order.tableNumber ? ' — Table ' + order.tableNumber : ''),
        icon: '/images/logo.png'
      });
    } else if ('Notification' in window && Notification.permission !== 'denied') {
      Notification.requestPermission();
    }
    // Simple audio beep using Web Audio API
    try {
      var ctx  = new (window.AudioContext || window.webkitAudioContext)();
      var osc  = ctx.createOscillator();
      var gain = ctx.createGain();
      osc.connect(gain); gain.connect(ctx.destination);
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      gain.gain.setValueAtTime(.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(.001, ctx.currentTime + .4);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + .4);
    } catch (_) {}
  }

  connectKDS();

})();
