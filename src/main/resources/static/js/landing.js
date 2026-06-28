/**
 * landing.js — Public-facing landing page
 * No frameworks. No login required.
 * Handles: nav scroll, menu browsing, reservation form.
 */

/* ═══════════════════════════════════════════════════════════
   NAV — becomes opaque on scroll, mobile hamburger
   ═══════════════════════════════════════════════════════════ */
(function initNav() {
  const nav        = document.getElementById('mainNav');
  const menuBtn    = document.getElementById('mobileMenuBtn');
  const navLinks   = document.getElementById('navLinks');

  // Always start opaque so the nav is visible (hero has its own bg)
  nav.classList.add('scrolled');

  window.addEventListener('scroll', () => {
    nav.classList.toggle('scrolled', window.scrollY > 60);
  }, { passive: true });

  menuBtn?.addEventListener('click', () => {
    navLinks.classList.toggle('open');
  });

  // Close mobile menu on link click
  navLinks?.querySelectorAll('.nav-link').forEach(a => {
    a.addEventListener('click', () => navLinks.classList.remove('open'));
  });
})();

/* ═══════════════════════════════════════════════════════════
   CSRF — fetched once, reused for the reservation POST
   ═══════════════════════════════════════════════════════════ */
let csrfToken  = '';
let csrfHeader = 'X-CSRF-TOKEN';

async function loadCsrf() {
  try {
    const res  = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' });
    const data = await res.json();
    csrfToken  = data.token  || '';
    csrfHeader = data.headerName || 'X-CSRF-TOKEN';
  } catch (_) { /* non-critical; POST will fail gracefully if CSRF absent */ }
}

/* ═══════════════════════════════════════════════════════════
   HELPERS
   ═══════════════════════════════════════════════════════════ */
function esc(str) {
  return String(str ?? '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function price(n) { return '£' + Number(n).toFixed(2); }
function fmtDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('en-GB', {
    weekday:'short', day:'numeric', month:'long', year:'numeric'
  });
}
function fmtTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleTimeString('en-GB', { hour:'2-digit', minute:'2-digit' });
}

/* ═══════════════════════════════════════════════════════════
   MENU
   ═══════════════════════════════════════════════════════════ */
let allItems   = [];
let activeCat  = '';
let searchTmr  = null;

async function initMenu() {
  await loadCategories();
  await fetchItems();
  bindMenuEvents();
}

async function loadCategories() {
  try {
    const cats = await fetch('/api/v1/categories').then(r => r.json());
    const row  = document.getElementById('categoryPills');
    (cats || []).forEach(cat => {
      const btn = document.createElement('button');
      btn.className   = 'pill';
      btn.dataset.cat = cat.id;
      btn.textContent = cat.name;
      btn.addEventListener('click', () => {
        row.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        activeCat = String(cat.id);
        fetchItems();
      });
      row.appendChild(btn);
    });
  } catch (_) {}
}

async function fetchItems() {
  const grid = document.getElementById('menuGrid');
  grid.innerHTML = '<div class="grid-loading">Loading menu…</div>';

  const q = document.getElementById('menuSearch')?.value.trim() || '';
  let url = '/api/v1/menu?available=true&size=100';
  if (activeCat) url += `&categoryId=${activeCat}`;
  if (q)         url += `&search=${encodeURIComponent(q)}`;

  try {
    const data = await fetch(url).then(r => r.json());
    allItems = data?.content || [];
    renderItems();
  } catch (_) {
    grid.innerHTML = '<div class="grid-empty">⚠️ Could not load menu. Please refresh.</div>';
  }
}

function renderItems() {
  const grid     = document.getElementById('menuGrid');
  const filterV  = document.getElementById('fVeg')?.checked;
  const filterVn = document.getElementById('fVegan')?.checked;
  const filterGf = document.getElementById('fGf')?.checked;

  let items = allItems;
  if (filterV)  items = items.filter(i => i.isVegetarian);
  if (filterVn) items = items.filter(i => i.isVegan);
  if (filterGf) items = items.filter(i => i.isGlutenFree);

  if (!items.length) {
    grid.innerHTML = '<div class="grid-empty">🔍 No dishes match your filters.</div>';
    return;
  }
  grid.innerHTML = items.map(cardHtml).join('');
}

function cardHtml(item) {
  const badges = [
    item.isVegetarian ? '<span class="badge badge-veg">🌿 Vegetarian</span>' : '',
    item.isVegan      ? '<span class="badge badge-vegan">🌱 Vegan</span>'    : '',
    item.isGlutenFree ? '<span class="badge badge-gf">🌾 Gluten-Free</span>' : '',
  ].filter(Boolean).join('');

  const imgEl = item.imageUrl
    ? `<img src="${esc(item.imageUrl)}" alt="${esc(item.name)}" loading="lazy"/>`
    : '🍽️';

  return `
    <article class="menu-card">
      <div class="card-img">${imgEl}</div>
      <div class="card-body">
        <div class="card-name">${esc(item.name)}</div>
        ${item.description ? `<div class="card-desc">${esc(item.description)}</div>` : ''}
        ${badges ? `<div class="badges">${badges}</div>` : ''}
        <div class="card-footer">
          <span class="card-price">${price(item.price)}</span>
          ${item.preparationTimeMinutes
            ? `<span class="card-meta">⏱ ${item.preparationTimeMinutes} min</span>` : ''}
        </div>
      </div>
    </article>`;
}

function bindMenuEvents() {
  document.getElementById('menuSearch')?.addEventListener('input', () => {
    clearTimeout(searchTmr);
    searchTmr = setTimeout(fetchItems, 350);
  });
  ['fVeg','fVegan','fGf'].forEach(id =>
    document.getElementById(id)?.addEventListener('change', renderItems));
}

/* ═══════════════════════════════════════════════════════════
   RESERVATION FORM
   ═══════════════════════════════════════════════════════════ */
let partySize = 2;

function initReserve() {
  // Date min = today
  const dateEl = document.getElementById('rDate');
  if (dateEl) dateEl.min = new Date().toISOString().split('T')[0];

  // Time slots 12:00–22:00 in 30-min steps
  const timeEl = document.getElementById('rTime');
  if (timeEl) {
    for (let h = 12; h <= 22; h++) {
      for (let m = 0; m < 60; m += 30) {
        if (h === 22 && m > 0) break;
        const hh = String(h).padStart(2,'0');
        const mm = String(m).padStart(2,'0');
        const opt = Object.assign(document.createElement('option'), {
          value: `${hh}:${mm}`, textContent: `${hh}:${mm}`
        });
        if (h === 19 && m === 0) opt.selected = true;
        timeEl.appendChild(opt);
      }
    }
  }

  // Party size stepper
  const valEl = document.getElementById('partyVal');
  document.getElementById('stepDown')?.addEventListener('click', () => {
    if (partySize > 1) valEl.textContent = --partySize;
  });
  document.getElementById('stepUp')?.addEventListener('click', () => {
    if (partySize < 12) valEl.textContent = ++partySize;
  });

  document.getElementById('reserveForm')?.addEventListener('submit', onReserveSubmit);

  // Modal close
  document.getElementById('modalClose')?.addEventListener('click', () => {
    document.getElementById('successModal').style.display = 'none';
  });
  document.getElementById('successModal')?.addEventListener('click', e => {
    if (e.target.id === 'successModal')
      document.getElementById('successModal').style.display = 'none';
  });
}

async function onReserveSubmit(e) {
  e.preventDefault();
  const btn     = document.getElementById('reserveBtn');
  const alertEl = document.getElementById('reserveAlert');

  const name  = document.getElementById('rName').value.trim();
  const phone = document.getElementById('rPhone').value.trim();
  const email = document.getElementById('rEmail').value.trim();
  const date  = document.getElementById('rDate').value;
  const time  = document.getElementById('rTime').value;
  const notes = document.getElementById('rNotes').value.trim();

  // Basic validation
  if (!name)  { showAlert(alertEl, 'Please enter your name.', 'error'); return; }
  if (!date)  { showAlert(alertEl, 'Please select a date.', 'error'); return; }
  if (!time)  { showAlert(alertEl, 'Please select a time.', 'error'); return; }

  alertEl.style.display = 'none';
  btn.disabled    = true;
  btn.textContent = 'Checking availability…';

  try {
    // Ensure we have a CSRF token
    if (!csrfToken) await loadCsrf();

    const payload = {
      customerName:  name,
      customerPhone: phone || null,
      customerEmail: email || null,
      partySize,
      reservedDate: `${date}T${time}:00`,
      notes: notes || null
    };

    btn.textContent = 'Confirming your booking…';

    const res = await fetch('/api/v1/reservations', {
      method:  'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type':  'application/json',
        'Accept':        'application/json',
        [csrfHeader]:    csrfToken
      },
      body: JSON.stringify(payload)
    });

    if (res.status === 401) { window.location.href = '/login'; return; }

    if (!res.ok) {
      let msg = `Booking failed (${res.status})`;
      try { const err = await res.json(); msg = err.message || msg; } catch (_) {}
      showAlert(alertEl, msg, 'error');
      return;
    }

    const booking = await res.json();

    // Show success modal
    document.getElementById('modalSummary').textContent =
      `Table ${booking.tableNumber} booked for ${partySize} guest${partySize !== 1 ? 's' : ''} ` +
      `on ${fmtDate(booking.reservedDate)} at ${fmtTime(booking.reservedDate)}.`;
    document.getElementById('modalCode').textContent = booking.confirmationCode;
    document.getElementById('successModal').style.display = 'flex';

    // Reset form
    e.target.reset();
    partySize = 2;
    document.getElementById('partyVal').textContent = '2';

  } catch (err) {
    showAlert(alertEl,
      err.message || 'Something went wrong. Please try again or call us directly.',
      'error');
  } finally {
    btn.disabled    = false;
    btn.textContent = 'Book My Table';
  }
}

function showAlert(el, msg, type) {
  el.textContent = msg;
  el.className   = `r-alert ${type}`;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/* ═══════════════════════════════════════════════════════════
   BOOT
   ═══════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  loadCsrf();   // preload token so form submit is instant
  initMenu();
  initReserve();
});
