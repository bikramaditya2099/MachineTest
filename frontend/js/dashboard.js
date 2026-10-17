/**
 * dashboard.js — Handles dashboard and product management UI.
 * Loads products with pagination, sorting, and filtering.
 * Allows CRUD operations for authenticated users.
 */

const API_BASE = 'http://localhost:8080/api';

// ── State ──────────────────────────────────────────────────────────────────
let currentPage = 0;
let totalPages  = 0;
const PAGE_SIZE = 10;
let editingProductId = null;
let searchTimer = null;

// ── Init ───────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  requireAuth();
  initDashboard();
});

function requireAuth() {
  if (!localStorage.getItem('jwt_token')) {
    window.location.href = 'index.html';
  }
}

function initDashboard() {
  const username = localStorage.getItem('username') || 'User';
  const userType = localStorage.getItem('user_type') || 'STANDARD_USER';

  // Set user info in sidebar
  setTextById('sidebar-username', username);
  setTextById('sidebar-role', userType === 'ADMINISTRATOR' ? '🔑 Administrator' : '👤 Standard User');
  setTextById('sidebar-avatar', username.charAt(0).toUpperCase());

  // Update welcome banner
  setTextById('welcome-name', `Welcome, ${username}! 👋`);
  setTextById('welcome-role', `Logged in as ${userType === 'ADMINISTRATOR' ? 'Administrator' : 'Standard User'}`);

  // Show/hide admin items
  const adminItems = document.querySelectorAll('.admin-only');
  adminItems.forEach(el => {
    el.style.display = userType === 'ADMINISTRATOR' ? '' : 'none';
  });

  // Load products
  loadProducts();

  // Search with debounce
  const searchInput = document.getElementById('product-search');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
        currentPage = 0;
        loadProducts();
      }, 400);
    });
  }
}

// ── Product Loading ────────────────────────────────────────────────────────

async function loadProducts(sortBy = 'id', sortDir = 'asc') {
  const token = localStorage.getItem('jwt_token');
  const nameFilter = document.getElementById('product-search')?.value || '';

  try {
    showTableLoading(true);
    const params = new URLSearchParams({
      page: currentPage,
      size: PAGE_SIZE,
      sortBy,
      sortDir,
      ...(nameFilter && { name: nameFilter })
    });

    const response = await fetch(`${API_BASE}/products?${params}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (response.status === 401) {
      handleUnauthorized();
      return;
    }

    const data = await response.json();

    if (data.success) {
      const pageData = data.data;
      totalPages = pageData.totalPages;
      renderProducts(pageData.content);
      renderPagination(pageData.totalElements);
      setTextById('product-count', pageData.totalElements);
    } else {
      showTableError(data.message);
    }
  } catch (err) {
    showTableError('Failed to load products. Check your connection.');
    console.error('Load products error:', err);
  } finally {
    showTableLoading(false);
  }
}

function renderProducts(products) {
  const tbody = document.getElementById('products-tbody');
  if (!tbody) return;

  if (!products || products.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align:center; padding: 48px; color: var(--text-muted);">
          <div style="font-size:40px; margin-bottom:12px;">📦</div>
          <div>No products found</div>
        </td>
      </tr>`;
    return;
  }

  tbody.innerHTML = products.map(p => `
    <tr>
      <td><span style="color:var(--text-muted); font-size:12px;">#${p.id}</span></td>
      <td><span style="font-weight:600">${escapeHtml(p.name)}</span></td>
      <td style="color:var(--text-secondary); font-size:13px; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
        ${escapeHtml(p.description || '—')}
      </td>
      <td><span style="color:#10b981; font-weight:700;">₹${parseFloat(p.price).toFixed(2)}</span></td>
      <td>
        <span class="badge ${p.quantity > 0 ? 'badge-success' : 'badge-admin'}">
          ${p.quantity} ${p.quantity === 1 ? 'unit' : 'units'}
        </span>
      </td>
      <td>
        <div class="flex gap-2">
          <button class="action-btn action-btn-edit" onclick="openEditModal(${p.id})">✏️ Edit</button>
          <button class="action-btn action-btn-delete" onclick="deleteProduct(${p.id}, '${escapeHtml(p.name)}')">🗑️ Delete</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function renderPagination(total) {
  const container = document.getElementById('pagination');
  if (!container) return;

  const pages = Math.ceil(total / PAGE_SIZE);
  totalPages = pages;

  let html = `<button class="page-btn" onclick="changePage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>‹</button>`;

  const start = Math.max(0, currentPage - 2);
  const end   = Math.min(pages - 1, currentPage + 2);

  for (let i = start; i <= end; i++) {
    html += `<button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="changePage(${i})">${i + 1}</button>`;
  }

  html += `<button class="page-btn" onclick="changePage(${currentPage + 1})" ${currentPage >= pages - 1 ? 'disabled' : ''}>›</button>`;
  container.innerHTML = html;
}

function changePage(page) {
  if (page < 0 || page >= totalPages) return;
  currentPage = page;
  loadProducts();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ── Product CRUD ───────────────────────────────────────────────────────────

function openCreateModal() {
  editingProductId = null;
  document.getElementById('modal-title').textContent = '➕ Add New Product';
  document.getElementById('product-form').reset();
  openModal('product-modal');
}

async function openEditModal(id) {
  const token = localStorage.getItem('jwt_token');
  try {
    const response = await fetch(`${API_BASE}/products/${id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    if (data.success) {
      const p = data.data;
      editingProductId = id;
      document.getElementById('modal-title').textContent = '✏️ Edit Product';
      document.getElementById('p-name').value        = p.name;
      document.getElementById('p-description').value = p.description || '';
      document.getElementById('p-price').value       = p.price;
      document.getElementById('p-quantity').value    = p.quantity;
      openModal('product-modal');
    }
  } catch (err) {
    console.error('Load product error:', err);
  }
}

async function handleProductSubmit(event) {
  event.preventDefault();

  const token   = localStorage.getItem('jwt_token');
  const name        = document.getElementById('p-name').value.trim();
  const description = document.getElementById('p-description').value.trim();
  const price       = parseFloat(document.getElementById('p-price').value);
  const quantity    = parseInt(document.getElementById('p-quantity').value);

  if (!name || isNaN(price) || isNaN(quantity)) {
    alert('Please fill all required fields correctly.');
    return;
  }

  const url    = editingProductId ? `${API_BASE}/products/${editingProductId}` : `${API_BASE}/products`;
  const method = editingProductId ? 'PUT' : 'POST';

  try {
    const response = await fetch(url, {
      method,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name, description, price, quantity })
    });

    const data = await response.json();
    if (data.success) {
      closeModal('product-modal');
      loadProducts();
    } else {
      alert(data.message || 'Operation failed');
    }
  } catch (err) {
    alert('Error: ' + err.message);
  }
}

async function deleteProduct(id, name) {
  if (!confirm(`Delete "${name}"? This action cannot be undone.`)) return;

  const token = localStorage.getItem('jwt_token');

  try {
    const response = await fetch(`${API_BASE}/products/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    const data = await response.json();
    if (data.success) {
      loadProducts();
    } else {
      alert(data.message || 'Delete failed');
    }
  } catch (err) {
    alert('Error: ' + err.message);
  }
}

// ── Modal Helpers ──────────────────────────────────────────────────────────

function openModal(id) {
  document.getElementById(id)?.classList.add('open');
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}

// ── Auth ───────────────────────────────────────────────────────────────────

function handleUnauthorized() {
  localStorage.clear();
  window.location.href = 'index.html';
}

async function handleLogout() {
  const token = localStorage.getItem('jwt_token');
  const username = localStorage.getItem('username') || 'User';

  try {
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    console.log(`${username} Logged Out`);
  } catch (err) {
    console.error(err);
  } finally {
    localStorage.clear();
    window.location.href = 'index.html';
  }
}

// ── UI Helpers ─────────────────────────────────────────────────────────────

function setTextById(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function showTableLoading(show) {
  const el = document.getElementById('table-loading');
  if (el) el.style.display = show ? 'block' : 'none';
}

function showTableError(msg) {
  const tbody = document.getElementById('products-tbody');
  if (tbody) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align:center; padding: 48px; color: var(--accent-danger);">
          ❌ ${escapeHtml(msg)}
        </td>
      </tr>`;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function setActivePage(pageName) {
  document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
  document.getElementById(`nav-${pageName}`)?.classList.add('active');
}
