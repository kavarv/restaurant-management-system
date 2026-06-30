/**
 * orders.js — New order form logic.
 * Manages the cart state, menu filtering/search, and order placement.
 * Loaded only on /orders/new (orders/form.html).
 */
(function () {
  'use strict';

  /* ── State ─────────────────────────────────────────────────────────── */
  var cart = {};          // { itemId: { id, name, price, qty, note } }
  var orderType = 'DINE_IN';
  var searchTimer = null;

  var CSRFToken  = document.getElementById('csrfToken')  ? document.getElementById('csrfToken').content  : '';
  var CSRFHeader = document.getElementById('csrfHeader') ? document.getElementById('csrfHeader').content : 'X-CSRF-TOKEN';

  /* ── Category filter ───────────────────────────────────────────────── */
  window.filterCategory = function (btn) {
    document.querySelectorAll('#categoryPills .pill').forEach(function (p) { p.classList.remove('active'); });
    btn.classList.add('active');
    var cat = btn.getAttribute('data-category');
    document.querySelectorAll('#menuGrid .menu-card').forEach(function (card) {
      if (cat === 'all') {
        card.style.display = '';
      } else {
        card.style.display = (card.getAttribute('data-category') === cat) ? '' : 'none';
      }
    });
    checkNoResults();
  };

  /* ── Search ────────────────────────────────────────────────────────── */
  window.debounceSearch = function (val) {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () { doSearch(val); }, 250);
  };

  function doSearch(val) {
    var lower = val.toLowerCase().trim();
    document.querySelectorAll('#menuGrid .menu-card').forEach(function (card) {
      var name = (card.getAttribute('data-name') || '').toLowerCase();
      card.style.display = (!lower || name.includes(lower)) ? '' : 'none';
    });
    checkNoResults();
  }

  function checkNoResults() {
    var visible = document.querySelectorAll('#menuGrid .menu-card:not([style*="none"])').length;
    var noRes = document.getElementById('noResults');
    if (noRes) noRes.style.display = visible === 0 ? 'block' : 'none';
  }

  /* ── Cart operations ───────────────────────────────────────────────── */
  window.addToCart = function (id, name, price) {
    if (cart[id]) {
      cart[id].qty++;
    } else {
      cart[id] = { id: id, name: name, price: parseFloat(price), qty: 1, note: '' };
    }
    renderCart();
  };

  function removeFromCart(id) {
    delete cart[id];
    renderCart();
  }

  function changeQty(id, delta) {
    if (!cart[id]) return;
    cart[id].qty += delta;
    if (cart[id].qty <= 0) { removeFromCart(id); return; }
    renderCart();
  }

  function setNote(id, val) {
    if (cart[id]) cart[id].note = val;
  }

  function renderCart() {
    var container = document.getElementById('cartItems');
    var empty     = document.getElementById('emptyCart');
    var placeBtn  = document.getElementById('placeOrderBtn');
    var ids       = Object.keys(cart);

    if (ids.length === 0) {
      empty.style.display = 'block';
      container.innerHTML = '';
      container.appendChild(empty);
      updateTotals(0);
      if (placeBtn) placeBtn.disabled = true;
      document.getElementById('cartItemCount').textContent = '0 items';
      return;
    }

    empty.style.display = 'none';
    var html = '';
    var subtotal = 0;

    ids.forEach(function (id) {
      var item = cart[id];
      var lineTotal = item.price * item.qty;
      subtotal += lineTotal;
      html += '<div class="cart-item">'
        + '<div>'
        +   '<div style="font-weight:600;font-size:var(--fs-sm);">' + escHtml(item.name) + '</div>'
        +   '<div style="font-size:var(--fs-xs);color:var(--color-text-muted);">RM ' + item.price.toFixed(2) + ' each</div>'
        +   '<input type="text" class="form-control" style="margin-top:4px;font-size:var(--fs-xs);padding:4px 8px;" '
        +     'placeholder="Special instructions..." value="' + escHtml(item.note) + '" '
        +     'onchange="setNote(' + id + ', this.value)"/>'
        + '</div>'
        + '<div class="qty-control">'
        +   '<button class="qty-btn" onclick="changeQty(' + id + ', -1)">-</button>'
        +   '<span class="qty-value">' + item.qty + '</span>'
        +   '<button class="qty-btn" onclick="changeQty(' + id + ', 1)">+</button>'
        + '</div>'
        + '<div style="text-align:right;">'
        +   '<div style="font-weight:700;font-size:var(--fs-sm);">RM ' + lineTotal.toFixed(2) + '</div>'
        +   '<button style="background:none;border:none;color:var(--color-danger);cursor:pointer;font-size:var(--fs-xs);" '
        +     'onclick="removeFromCart(' + id + ')">Remove</button>'
        + '</div>'
        + '</div>';
    });

    container.innerHTML = html;
    updateTotals(subtotal);
    document.getElementById('cartItemCount').textContent = ids.length + ' item' + (ids.length > 1 ? 's' : '');
    if (placeBtn) placeBtn.disabled = false;
  }

  // Expose for inline onclick
  window.removeFromCart = removeFromCart;
  window.changeQty      = changeQty;
  window.setNote        = setNote;

  function updateTotals(subtotal) {
    var tax   = subtotal * 0.10;
    var total = subtotal + tax;
    document.getElementById('subtotal').textContent = 'RM ' + subtotal.toFixed(2);
    document.getElementById('tax').textContent      = 'RM ' + tax.toFixed(2);
    document.getElementById('total').textContent    = 'RM ' + total.toFixed(2);
  }

  /* ── Order type toggle ─────────────────────────────────────────────── */
  window.setOrderType = function (type) {
    orderType = type;
    document.getElementById('btnDineIn').classList.toggle('active',  type === 'DINE_IN');
    document.getElementById('btnTakeaway').classList.toggle('active', type === 'TAKEAWAY');
    var tg = document.getElementById('tableGroup');
    if (tg) tg.style.display = type === 'DINE_IN' ? '' : 'none';
  };

  /* ── Order confirmation modal ──────────────────────────────────────── */
  window.openOrderConfirm = function () {
    var ids = Object.keys(cart);
    if (ids.length === 0) return;

    var tableId  = document.getElementById('tableSelect') ? document.getElementById('tableSelect').value : '';
    var tableText = document.getElementById('tableSelect') && tableId
      ? document.getElementById('tableSelect').options[document.getElementById('tableSelect').selectedIndex].text
      : 'Takeaway';

    if (orderType === 'DINE_IN' && !tableId) {
      alert('Please select a table first.');
      return;
    }

    var subtotal = 0;
    ids.forEach(function (id) { subtotal += cart[id].price * cart[id].qty; });
    var tax   = subtotal * 0.10;
    var total = subtotal + tax;

    var itemsHtml = ids.map(function (id) {
      return '<div style="display:flex;justify-content:space-between;font-size:var(--fs-sm);padding:2px 0;">'
        + '<span>' + cart[id].qty + 'x ' + escHtml(cart[id].name) + '</span>'
        + '<span>RM ' + (cart[id].price * cart[id].qty).toFixed(2) + '</span></div>';
    }).join('');

    document.getElementById('confirmModalBody').innerHTML =
      '<div style="margin-bottom:12px;"><strong>Type:</strong> ' + orderType.replace('_', ' ') + '</div>'
      + '<div style="margin-bottom:12px;"><strong>Table:</strong> ' + tableText + '</div>'
      + '<div class="card" style="padding:var(--space-4);margin-bottom:12px;">' + itemsHtml + '</div>'
      + '<div style="display:flex;justify-content:space-between;font-weight:700;font-size:var(--fs-md);">'
      + '<span>Total (incl. 10% tax)</span><span>RM ' + total.toFixed(2) + '</span></div>';

    openModal('confirmModal');
  };

  /* ── Place order ───────────────────────────────────────────────────── */
  window.placeOrder = function () {
    var ids = Object.keys(cart);
    if (ids.length === 0) return;

    var tableId = document.getElementById('tableSelect') ? document.getElementById('tableSelect').value : null;
    var items = ids.map(function (id) {
      return { menuItemId: parseInt(id), quantity: cart[id].qty, specialInstructions: cart[id].note || '' };
    });

    var payload = {
      tableId:   tableId ? parseInt(tableId) : null,
      orderType: orderType,
      items:     items
    };

    var confirmBtn = document.getElementById('confirmPlaceBtn');
    if (confirmBtn) { confirmBtn.disabled = true; confirmBtn.classList.add('loading'); }

    var headers = { 'Content-Type': 'application/json' };
    headers[CSRFHeader] = CSRFToken;

    fetch('/api/v1/orders', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload),
      credentials: 'same-origin'
    })
    .then(function (r) {
      if (!r.ok) return r.json().then(function(e) { throw new Error(e.message || 'Failed to place order'); });
      return r.json();
    })
    .then(function (data) {
      window.location.href = '/orders/' + data.id;
    })
    .catch(function (err) {
      closeModal('confirmModal');
      var errDiv = document.createElement('div');
      errDiv.className = 'alert alert-danger';
      errDiv.textContent = err.message;
      document.querySelector('main').prepend(errDiv);
      if (confirmBtn) { confirmBtn.disabled = false; confirmBtn.classList.remove('loading'); }
    });
  };

  /* ── Utilities ─────────────────────────────────────────────────────── */
  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

})();
