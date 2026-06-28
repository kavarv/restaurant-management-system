/**
 * customer.js — Customer Panel
 * Single file for all 4 customer pages.
 * Page detection: document.body.dataset.page
 * No frameworks, no build step — plain ES2020+.
 */

/* ── CSRF helper ─────────────────────────────────────────────────────────── */
function getCsrf() {
  return {
    token:  document.querySelector('meta[name="_csrf"]')?.content ?? '',
    header: document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN'
  };
}

/* ── Fetch wrapper with error handling ───────────────────────────────────── */
async function apiFetch(url, options = {}) {
  const csrf = getCsrf();
  const method = (options.method || 'GET').toUpperCase();
  const headers = { 'Accept': 'application/json', ...(options.headers || {}) };

  if (['POST', 'PATCH', 'PUT', 'DELETE'].includes(method)) {
    headers[csrf.header] = csrf.token;
    if (options.body && typeof options.body === 'object') {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }
  }

  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) { window.location.href = '/login'; return null; }
  if (res.status === 403) { throw new Error('Access denied (403)'); }
  if (!res.ok) {
    let msg = `Error ${res.status}`;
    try { const err = await res.json(); msg = err.message || msg; } catch (_) {}
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

/* ── Format helpers ──────────────────────────────────────────────────────── */
function formatDate(isoStr) {
  if (!isoStr) return '—';
  const d = new Date(isoStr);
  return d.toLocaleDateString('en-GB', { weekday:'short', day:'numeric', month:'short', year:'numeric' });
}
function formatTime(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}
function formatPrice(price) {
  return '£' + Number(price).toFixed(2);
}
function escHtml(str) {
  return String(str ?? '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

/* ═══════════════════════════════════════════════════════════════════════════
   PAGE: DASHBOARD
   ═══════════════════════════════════════════════════════════════════════════ */
async function initDashboard() {
  await loadSpecials();
}

async function loadSpecials() {
  const grid = document.getElementById('specialsGrid');
  if (!grid) return;

  try {
    // Fetch categories to find "Specials" id
    const cats = await apiFetch('/api/v1/categories');
    const specialsCat = (cats || []).find(c =>
      c.name.toLowerCase().includes('special'));

    let items = [];
    if (specialsCat) {
      const data = await apiFetch(
        `/api/v1/menu?available=true&categoryId=${specialsCat.id}&size=3`);
      items = data?.content || [];
    }

    if (!items.length) {
      // Fall back: just fetch first 3 available items
      const data = await apiFetch('/api/v1/menu?available=true&size=3');
      items = data?.content || [];
    }

    grid.innerHTML = items.length
      ? items.map(renderMenuCard).join('')
      : '<p class="loading-spinner" style="padding:24px;">No specials today.</p>';
  } catch (err) {
    grid.innerHTML = `<p class="loading-spinner" style="padding:24px;">Could not load specials.</p>`;
    console.error(err);
  }
}

/* ═══════════════════════════════════════════════════════════════════════════
   PAGE: MENU
   ═══════════════════════════════════════════════════════════════════════════ */
let allMenuItems  = [];
let activeCatId   = '';
let searchTimer   = null;

async function initMenu() {
  await loadCategories();
  await loadMenuItems();
  bindMenuFilters();
}

async function loadCategories() {
  const container = document.getElementById('categoryPills');
  if (!container) return;
  try {
    const cats = await apiFetch('/api/v1/categories');
    (cats || []).forEach(cat => {
      const btn = document.createElement('button');
      btn.className = 'pill';
      btn.dataset.cat = cat.id;
      btn.textContent = cat.name;
      btn.addEventListener('click', () => {
        document.querySelectorAll('.category-pills .pill').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        activeCatId = String(cat.id);
        loadMenuItems();
      });
      container.appendChild(btn);
    });
  } catch (err) { console.error('Failed to load categories', err); }
}

async function loadMenuItems() {
  const grid = document.getElementById('menuGrid');
  if (!grid) return;
  grid.innerHTML = '<div class="loading-spinner">Loading menu…</div>';

  try {
    const search = document.getElementById('menuSearch')?.value.trim() || '';
    let url = `/api/v1/menu?available=true&size=100`;
    if (activeCatId) url += `&categoryId=${activeCatId}`;
    if (search)      url += `&search=${encodeURIComponent(search)}`;

    const data = await apiFetch(url);
    allMenuItems = data?.content || [];
    renderFilteredItems();
  } catch (err) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⚠️</div><p>Failed to load menu.</p></div>';
    console.error(err);
  }
}

function renderFilteredItems() {
  const grid = document.getElementById('menuGrid');
  const filterVeg   = document.getElementById('filterVeg')?.checked;
  const filterVegan = document.getElementById('filterVegan')?.checked;
  const filterGf    = document.getElementById('filterGf')?.checked;

  let items = allMenuItems;
  if (filterVeg)   items = items.filter(i => i.isVegetarian);
  if (filterVegan) items = items.filter(i => i.isVegan);
  if (filterGf)    items = items.filter(i => i.isGlutenFree);

  if (!items.length) {
    grid.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">🔍</div>
        <p>No dishes match your filters.</p>
      </div>`;
    return;
  }
  grid.innerHTML = items.map(renderMenuCard).join('');
}

function renderMenuCard(item) {
  const badges = [
    item.isVegetarian ? '<span class="badge badge-veg">🌿 Vegetarian</span>' : '',
    item.isVegan      ? '<span class="badge badge-vegan">🌱 Vegan</span>'    : '',
    item.isGlutenFree ? '<span class="badge badge-gf">🌾 Gluten-Free</span>' : '',
  ].filter(Boolean).join('');

  const imgHtml = item.imageUrl
    ? `<div class="menu-card-img"><img src="${escHtml(item.imageUrl)}" alt="${escHtml(item.name)}" loading="lazy"/></div>`
    : `<div class="menu-card-img no-img"></div>`;

  const prepTime = item.preparationTimeMinutes
    ? `⏱ ${item.preparationTimeMinutes} min` : '';

  return `
    <div class="menu-card">
      ${imgHtml}
      <div class="menu-card-body">
        <div class="menu-card-name">${escHtml(item.name)}</div>
        ${item.description ? `<div class="menu-card-desc">${escHtml(item.description)}</div>` : ''}
        ${badges ? `<div class="badges">${badges}</div>` : ''}
        <div class="menu-card-footer">
          <span class="menu-card-price">${formatPrice(item.price)}</span>
          ${prepTime ? `<span class="menu-card-meta">${prepTime}</span>` : ''}
        </div>
      </div>
    </div>`;
}

function bindMenuFilters() {
  const searchInput = document.getElementById('menuSearch');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(loadMenuItems, 350);
    });
  }
  ['filterVeg','filterVegan','filterGf'].forEach(id => {
    document.getElementById(id)?.addEventListener('change', renderFilteredItems);
  });
}

/* ═══════════════════════════════════════════════════════════════════════════
   PAGE: RESERVE
   ═══════════════════════════════════════════════════════════════════════════ */
let partySize = 2;

async function initReserve() {
  // Set date min to today
  const dateInput = document.getElementById('resDate');
  if (dateInput) {
    const today = new Date().toISOString().split('T')[0];
    dateInput.min = today;
  }

  // Populate time slots (12:00–22:00, 30-min steps)
  const timeSelect = document.getElementById('resTime');
  if (timeSelect) {
    for (let h = 12; h <= 22; h++) {
      for (let m = 0; m < 60; m += 30) {
        if (h === 22 && m > 0) break;
        const hh = String(h).padStart(2, '0');
        const mm = String(m).padStart(2, '0');
        const opt = document.createElement('option');
        opt.value = `${hh}:${mm}`;
        opt.textContent = `${hh}:${mm}`;
        if (h === 19 && m === 0) opt.selected = true;
        timeSelect.appendChild(opt);
      }
    }
  }

  // Party size stepper
  const partyEl = document.getElementById('partyValue');
  document.getElementById('decreaseParty')?.addEventListener('click', () => {
    if (partySize > 1) { partySize--; partyEl.textContent = partySize; }
  });
  document.getElementById('increaseParty')?.addEventListener('click', () => {
    if (partySize < 12) { partySize++; partyEl.textContent = partySize; }
  });

  // Form submit
  document.getElementById('reserveForm')?.addEventListener('submit', handleReserveSubmit);
}

async function handleReserveSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('reserveBtn');
  const alertEl = document.getElementById('reserveAlert');

  const date  = document.getElementById('resDate').value;
  const time  = document.getElementById('resTime').value;
  const notes = document.getElementById('resNotes').value.trim();

  if (!date || !time) {
    showAlert(alertEl, 'Please select a date and time.', 'error');
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Finding a table…';
  alertEl.style.display = 'none';

  try {
    // Step 1: find an available table
    const tables = await apiFetch('/api/v1/tables/available');
    const suitable = (tables || []).filter(t => t.capacity >= partySize);
    if (!suitable.length) {
      showAlert(alertEl, 'Sorry, no available tables for that party size right now. Please call us directly.', 'error');
      return;
    }
    const tableId = suitable[0].id;

    // Step 2: POST reservation
    const reservedDate = `${date}T${time}:00`;
    const payload = { tableId, reservedDate, partySize, notes: notes || null };

    btn.textContent = 'Booking your table…';
    const res = await apiFetch('/api/v1/reservations', { method: 'POST', body: payload });

    // Step 3: show success
    document.getElementById('reserveForm').style.display = 'none';
    const successEl = document.getElementById('reserveSuccess');
    successEl.style.display = 'block';
    document.getElementById('successSummary').textContent =
      `Table ${res.tableNumber} booked for ${partySize} on ${formatDate(res.reservedDate)} at ${formatTime(res.reservedDate)}. ` +
      `Confirmation: ${res.confirmationCode}`;
  } catch (err) {
    showAlert(alertEl, err.message || 'Booking failed. Please try again.', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Book Table';
  }
}

function showAlert(el, msg, type) {
  el.textContent = msg;
  el.className = `reserve-alert ${type}`;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/* ═══════════════════════════════════════════════════════════════════════════
   PAGE: MY RESERVATIONS
   ═══════════════════════════════════════════════════════════════════════════ */
async function initReservations() {
  await loadMyReservations();
}

async function loadMyReservations() {
  const list = document.getElementById('reservationsList');
  if (!list) return;
  list.innerHTML = '<div class="loading-spinner">Loading your bookings…</div>';

  try {
    const data = await apiFetch('/api/v1/reservations/my?size=50');
    const items = data?.content || [];

    if (!items.length) {
      list.innerHTML = `
        <div class="empty-bookings">
          <div class="empty-bookings-icon">📅</div>
          <h3>No bookings yet</h3>
          <p>Ready to reserve a table? We'd love to have you.</p>
          <a href="/customer/reserve" class="btn-primary" style="margin-top:20px;">Make a Reservation</a>
        </div>`;
      return;
    }

    list.innerHTML = items.map(renderBookingCard).join('');

    // Attach cancel listeners
    list.querySelectorAll('[data-cancel-id]').forEach(btn => {
      btn.addEventListener('click', () => handleCancel(btn.dataset.cancelId, btn));
    });
  } catch (err) {
    list.innerHTML = `<div class="empty-state"><p>Failed to load bookings: ${escHtml(err.message)}</p></div>`;
  }
}

function renderBookingCard(r) {
  const canCancel = r.status === 'PENDING' || r.status === 'CONFIRMED';
  const cancelBtn = canCancel
    ? `<button class="btn-danger" data-cancel-id="${r.id}">Cancel</button>`
    : '';

  const statusDot = {
    PENDING: '🟡', CONFIRMED: '🟢', CANCELLED: '⚫', COMPLETED: '🟣'
  }[r.status] || '';

  return `
    <div class="booking-card" id="booking-${r.id}">
      <div class="booking-info">
        <div class="booking-date">${formatDate(r.reservedDate)}</div>
        <div class="booking-details">
          <span class="booking-detail-item">🕐 ${formatTime(r.reservedDate)}</span>
          <span class="booking-detail-item">👥 ${r.partySize} guest${r.partySize !== 1 ? 's' : ''}</span>
          ${r.tableNumber ? `<span class="booking-detail-item">🪑 Table ${r.tableNumber}</span>` : ''}
        </div>
        ${r.notes ? `<div class="booking-notes">"${escHtml(r.notes)}"</div>` : ''}
        <div class="booking-code">Ref: ${escHtml(r.confirmationCode || '—')}</div>
      </div>
      <div class="booking-actions">
        <span class="status-badge ${escHtml(r.status)}">${statusDot} ${escHtml(r.status)}</span>
        ${cancelBtn}
      </div>
    </div>`;
}

async function handleCancel(reservationId, btn) {
  if (!confirm('Are you sure you want to cancel this reservation?')) return;

  btn.disabled = true;
  btn.textContent = 'Cancelling…';
  try {
    await apiFetch(`/api/v1/reservations/${reservationId}/cancel`, { method: 'PATCH' });
    // Refresh the list
    await loadMyReservations();
  } catch (err) {
    alert('Could not cancel: ' + err.message);
    btn.disabled = false;
    btn.textContent = 'Cancel';
  }
}

/* ═══════════════════════════════════════════════════════════════════════════
   ROUTER — run the right init function based on data-page
   ═══════════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  const page = document.body.dataset.page;
  switch (page) {
    case 'dashboard':    initDashboard();    break;
    case 'menu':         initMenu();         break;
    case 'reserve':      initReserve();      break;
    case 'reservations': initReservations(); break;
  }
});
