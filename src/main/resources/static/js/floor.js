/**
 * floor.js — Floor view interactions and WebSocket real-time updates.
 * Loaded only on /tables (tables/floor.html).
 */
(function () {
  'use strict';

  var csrf       = document.querySelector('meta[name="_csrf"]')        ? document.querySelector('meta[name="_csrf"]').content        : '';
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]') ? document.querySelector('meta[name="_csrf_header"]').content : 'X-CSRF-TOKEN';

  /* ── Table tile click ──────────────────────────────────────────────── */
  window.handleTableClick = function (tile) {
    var tableId = tile.getAttribute('data-table-id');
    var status  = tile.getAttribute('data-status');

    document.getElementById('modalTitle').textContent = 'Table ' + tile.querySelector('.table-tile-number').textContent;
    document.getElementById('modalBody').innerHTML = '<div class="loading-spinner" style="margin:32px auto;display:block;"></div>';
    openModal('tableModal');

    if (status === 'OCCUPIED') {
      // Load current order for this table
      fetch('/api/v1/tables/' + tableId + '/current-order', {
        headers: { [csrfHeader]: csrf }
      })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (order) {
        if (!order) {
          document.getElementById('modalBody').innerHTML =
            '<p class="text-muted text-sm">No active order for this table.</p>';
          return;
        }
        var itemsHtml = (order.items || []).map(function (item) {
          return '<div style="display:flex;justify-content:space-between;font-size:var(--fs-sm);padding:3px 0;">'
            + '<span>' + item.quantity + 'x ' + item.menuItemName + '</span>'
            + '<span>RM ' + parseFloat(item.unitPrice * item.quantity).toFixed(2) + '</span></div>';
        }).join('');
        document.getElementById('modalBody').innerHTML =
          '<div style="margin-bottom:12px;">'
          + '<span class="badge badge-' + order.status + '">' + order.status + '</span>'
          + '&nbsp; Order <strong>' + order.orderNumber + '</strong></div>'
          + '<div style="margin-bottom:12px;">' + itemsHtml + '</div>'
          + '<div style="display:flex;justify-content:space-between;font-weight:700;border-top:1px solid var(--color-border);padding-top:8px;">'
          + '<span>Total</span><span>RM ' + parseFloat(order.totalAmount || 0).toFixed(2) + '</span></div>'
          + '<div style="margin-top:12px;">'
          + '<a href="/orders/' + order.id + '" class="btn btn-primary btn-sm">View Order</a></div>';
        if (document.getElementById('changeStatusBtn')) {
          document.getElementById('changeStatusBtn').setAttribute('data-table-id', tableId);
        }
      });
    } else {
      document.getElementById('modalBody').innerHTML =
        '<p class="text-sm text-muted">Status: <span class="badge badge-' + status + '">' + status + '</span></p>'
        + (status === 'AVAILABLE'
            ? '<a href="/orders/new" class="btn btn-primary btn-sm" style="margin-top:12px;">New Order for this Table</a>'
            : '');
    }
  };

  /* ── Status change (Admin/Manager) ────────────────────────────────── */
  window.openStatusChange = function () {
    var statusTableId = document.getElementById('changeStatusBtn')
      ? document.getElementById('changeStatusBtn').getAttribute('data-table-id') : '';
    if (document.getElementById('statusTableId')) {
      document.getElementById('statusTableId').value = statusTableId;
    }
    closeModal('tableModal');
    openModal('statusModal');
  };

  window.submitStatusChange = function () {
    var tableId  = document.getElementById('statusTableId') ? document.getElementById('statusTableId').value : '';
    var newStatus = document.getElementById('newStatus') ? document.getElementById('newStatus').value : '';
    if (!tableId || !newStatus) return;

    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrf;

    fetch('/api/v1/tables/' + tableId + '/status', {
      method: 'PUT',
      headers: headers,
      body: JSON.stringify({ status: newStatus }),
      credentials: 'same-origin'
    })
    .then(function (r) {
      if (r.ok) {
        closeModal('statusModal');
        // Update tile in DOM immediately (also confirmed by WS)
        updateTileStatus(tableId, newStatus);
      }
    });
  };

  /* ── WebSocket real-time floor updates ─────────────────────────────── */
  window.onFloorUpdate = function (data) {
    if (data && data.tableId && data.status) {
      updateTileStatus(data.tableId, data.status);
    }
  };

  function updateTileStatus(tableId, status) {
    var tile = document.querySelector('[data-table-id="' + tableId + '"]');
    if (!tile) return;
    // Remove all status classes
    ['AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE'].forEach(function (s) {
      tile.classList.remove('status-' + s);
    });
    tile.classList.add('status-' + status);
    tile.setAttribute('data-status', status);
    // Update badge
    var badge = tile.querySelector('.badge');
    if (badge) {
      badge.className = 'badge badge-' + status;
      badge.textContent = status;
    }
  }

  /* ── Modal helpers (in case main.js not loaded yet) ───────────────── */
  if (!window.openModal) {
    window.openModal  = function (id) { var m = document.getElementById(id); if (m) m.classList.add('open'); };
    window.closeModal = function (id) { var m = document.getElementById(id); if (m) m.classList.remove('open'); };
    document.querySelectorAll('.modal-backdrop').forEach(function (b) {
      b.addEventListener('click', function (e) { if (e.target === b) b.classList.remove('open'); });
    });
  }

})();
