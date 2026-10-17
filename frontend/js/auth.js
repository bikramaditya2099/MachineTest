/**
 * auth.js — Handles login, registration, and OTP verification.
 * Communicates with Spring Boot backend via fetch API.
 */

const API_BASE = 'http://localhost:8080/api';

// ── Utility Functions ──────────────────────────────────────────────────────

/** Stores JWT token and user info in localStorage */
function saveSession(token, username, userType) {
  localStorage.setItem('jwt_token', token);
  localStorage.setItem('username', username);
  localStorage.setItem('user_type', userType);
}

/** Retrieves stored JWT token */
function getToken() { return localStorage.getItem('jwt_token'); }

/** Clears session storage */
function clearSession() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('username');
  localStorage.removeItem('user_type');
}

/** Redirects to login if not authenticated */
function requireAuth() {
  if (!getToken()) {
    window.location.href = 'index.html';
  }
}

/** Redirects to dashboard if already authenticated */
function redirectIfAuth() {
  if (getToken()) {
    window.location.href = 'dashboard.html';
  }
}

/** Shows an alert element */
function showAlert(id, message, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = `alert alert-${type} visible`;
  el.innerHTML = `<span>${type === 'success' ? '✅' : type === 'info' ? 'ℹ️' : '❌'}</span> ${message}`;
}

/** Hides an alert element */
function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.className = 'alert';
}

/** Sets button loading state */
function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  const spinner = btn.querySelector('.spinner');
  if (loading) {
    btn.disabled = true;
    if (spinner) spinner.classList.add('visible');
  } else {
    btn.disabled = false;
    if (spinner) spinner.classList.remove('visible');
  }
}

// ── Validation ─────────────────────────────────────────────────────────────

/** Validates email contains @ and . */
function validateEmail(email) {
  return email.includes('@') && email.includes('.');
}

/** Validates password complexity (8–12 chars, uppercase, lowercase, digit, special) */
function validatePassword(password) {
  const pattern = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&_#^()\-+=])[A-Za-z\d@$!%*?&_#^()\-+=]{8,12}$/;
  return pattern.test(password);
}

/** Validates username length (8–50) */
function validateUsername(username) {
  return username.length >= 8 && username.length <= 50;
}

/** Returns password strength level (0-4) */
function getPasswordStrength(password) {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[@$!%*?&_#^()\-+=]/.test(password)) score++;
  return score;
}

/** Shows password strength indicator */
function updatePasswordStrength(password, containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.style.display = 'block';

  const fill = container.querySelector('.strength-fill');
  const text = container.querySelector('.strength-text');
  const score = getPasswordStrength(password);

  const levels = [
    { color: '#ef4444', label: 'Very Weak', width: '20%' },
    { color: '#f59e0b', label: 'Weak', width: '40%' },
    { color: '#f59e0b', label: 'Fair', width: '60%' },
    { color: '#10b981', label: 'Strong', width: '80%' },
    { color: '#10b981', label: 'Very Strong', width: '100%' }
  ];

  const level = levels[Math.max(0, score - 1)] || levels[0];
  fill.style.width = score === 0 ? '0%' : level.width;
  fill.style.background = level.color;
  text.textContent = score === 0 ? '' : `Strength: ${level.label}`;
}

// ── Login Page ─────────────────────────────────────────────────────────────

async function handleLogin(event) {
  event.preventDefault();
  hideAlert('login-alert');

  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;

  if (!username || !password) {
    showAlert('login-alert', 'Please fill in all fields.');
    return;
  }

  setLoading('login-btn', true);

  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (response.ok && data.success) {
      const { token, userType, message } = data.data;
      saveSession(token, username, userType);
      showAlert('login-alert', message || `Welcome ${username}`, 'success');

      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 800);
    } else {
      showAlert('login-alert', data.message || 'Login Failed');
    }
  } catch (err) {
    showAlert('login-alert', 'Unable to connect to server. Please try again.');
    console.error('Login error:', err);
  } finally {
    setLoading('login-btn', false);
  }
}

// ── Registration Page ──────────────────────────────────────────────────────

let pendingOtpUsername = null;

async function handleRegister(event) {
  event.preventDefault();
  hideAlert('register-alert');

  const username = document.getElementById('reg-username').value.trim();
  const email    = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  const userType = document.getElementById('reg-usertype').value;
  const country  = document.getElementById('reg-country').value;

  // Client-side validation
  if (!validateUsername(username)) {
    showAlert('register-alert', 'Username must be 8–50 characters.');
    return;
  }
  if (!validateEmail(email)) {
    showAlert('register-alert', "Email must contain '@' and '.'");
    return;
  }
  if (!validatePassword(password)) {
    showAlert('register-alert', 'Password must be 8–12 chars with ≥1 uppercase, lowercase, digit, and special character.');
    return;
  }
  if (!userType) {
    showAlert('register-alert', 'Please select a user type.');
    return;
  }

  if (!country) {
    showAlert('register-alert', 'Please select your country.');
    return;
  }
  if (restrictedCountryCodes.has(country)) {
    showAlert('register-alert', 'Registration is not allowed from ' + selectedCountryName() + '.');
    return;
  }

  setLoading('register-btn', true);

  try {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password, userType, country })
    });

    const data = await response.json();

    if (response.ok && data.success) {
      const otp = data.data?.otp;
      pendingOtpUsername = username;

      // Show OTP display
      const otpBox = document.getElementById('otp-display');
      const otpCode = document.getElementById('otp-code');
      if (otpBox && otp) {
        otpCode.textContent = otp;
        otpBox.classList.add('visible');
      }

      // Show OTP verification section
      document.getElementById('reg-section').classList.add('hidden');
      document.getElementById('otp-section').classList.remove('hidden');
      document.getElementById('otp-username').textContent = username;

      showAlert('register-alert', 'Registration successful! Enter the OTP to activate your account.', 'success');
    } else {
      showAlert('register-alert', data.message || 'Registration failed. Please try again.');
    }
  } catch (err) {
    showAlert('register-alert', 'Unable to connect to server. Please try again.');
    console.error('Register error:', err);
  } finally {
    setLoading('register-btn', false);
  }
}

async function handleVerifyOtp(event) {
  event.preventDefault();
  hideAlert('otp-alert');

  const otp = document.getElementById('otp-input').value.trim();
  const username = pendingOtpUsername;

  if (!otp || otp.length !== 6) {
    showAlert('otp-alert', 'Please enter the 6-digit OTP.');
    return;
  }

  setLoading('otp-btn', true);

  try {
    const response = await fetch(`${API_BASE}/auth/verify-otp`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, otp })
    });

    const data = await response.json();

    if (response.ok && data.success) {
      showAlert('otp-alert', 'Account verified! Redirecting to login...', 'success');
      setTimeout(() => { window.location.href = 'index.html'; }, 1500);
    } else {
      showAlert('otp-alert', data.message || 'Invalid OTP. Please try again.');
    }
  } catch (err) {
    showAlert('otp-alert', 'Unable to connect to server.');
    console.error('OTP error:', err);
  } finally {
    setLoading('otp-btn', false);
  }
}

// ── Logout ─────────────────────────────────────────────────────────────────

async function handleLogout() {
  const token = getToken();
  if (!token) {
    window.location.href = 'index.html';
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();
    console.log('Logout:', data.message);
  } catch (err) {
    console.error('Logout error:', err);
  } finally {
    clearSession();
    window.location.href = 'index.html';
  }
}

// ── Password Toggle ────────────────────────────────────────────────────────

function togglePassword(inputId, iconId) {
  const input = document.getElementById(inputId);
  const icon  = document.getElementById(iconId);
  if (!input) return;

  if (input.type === 'password') {
    input.type = 'text';
    if (icon) icon.textContent = '🙈';
  } else {
    input.type = 'password';
    if (icon) icon.textContent = '👁️';
  }
}

// ── Country selection (sign-up) ──────────────────────────────────────────────

/** ISO codes of countries blocked from registering (filled from the backend). */
const restrictedCountryCodes = new Set();

function selectedCountryName() {
  const sel = document.getElementById('reg-country');
  return sel && sel.selectedIndex >= 0 ? sel.options[sel.selectedIndex].dataset.name || sel.value : '';
}

/**
 * Detects the visitor's country code. Tries, in order:
 *  1. Browser-side IP geolocation (ipapi.co, HTTPS)
 *  2. Backend IP geolocation (/auth/detect-country)
 *  3. Region from the browser locale (e.g. en-IN -> IN)
 */
async function detectUserCountry() {
  try {
    const r = await fetch('https://ipapi.co/json/');
    if (r.ok) {
      const j = await r.json();
      if (j.country_code) return j.country_code.toUpperCase();
    }
  } catch (e) { /* fall through */ }

  try {
    const r = await fetch(`${API_BASE}/auth/detect-country`);
    const j = await r.json();
    if (j.success && j.data && j.data.code) return j.data.code;
  } catch (e) { /* fall through */ }

  const lang = (navigator.languages && navigator.languages[0]) || navigator.language || '';
  const m = lang.match(/-([A-Za-z]{2})$/);
  return m ? m[1].toUpperCase() : null;
}

/** Loads the country list into the dropdown and pre-selects the detected country. */
async function initCountrySelect() {
  const select = document.getElementById('reg-country');
  if (!select) return;

  try {
    const res = await fetch(`${API_BASE}/auth/countries`);
    const json = await res.json();
    const countries = json.data || [];

    select.innerHTML = '<option value="" disabled>Select your country</option>';
    countries.forEach(c => {
      if (c.restricted) restrictedCountryCodes.add(c.code);
      const opt = document.createElement('option');
      opt.value = c.code;
      opt.dataset.name = c.name;
      opt.textContent = c.restricted ? c.name + ' (not available)' : c.name;
      select.appendChild(opt);
    });

    const detected = await detectUserCountry();
    const hint = document.getElementById('country-hint');
    if (detected && select.querySelector('option[value="' + detected + '"]')) {
      select.value = detected;
      if (hint) hint.textContent = 'Detected from your location — you can change it.';
    } else {
      select.selectedIndex = 0;
      if (hint) hint.textContent = 'Could not detect your location. Please select your country.';
    }
    onCountryChange();
  } catch (e) {
    select.innerHTML = '<option value="" disabled selected>Could not load countries</option>';
  }
}

/** Shows an error and disables sign-up when a restricted country is selected. */
function onCountryChange() {
  const code = document.getElementById('reg-country').value;
  const err = document.getElementById('country-error');
  const btn = document.getElementById('register-btn');
  const blocked = restrictedCountryCodes.has(code);
  if (err) {
    err.textContent = blocked ? 'Registration is not allowed from ' + selectedCountryName() + '.' : '';
    err.classList.toggle('visible', blocked);
  }
  if (btn) btn.disabled = blocked;
}

document.addEventListener('DOMContentLoaded', initCountrySelect);
