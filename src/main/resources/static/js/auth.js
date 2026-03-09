// auth.js: Handles login, registration, and logout for SEPM Project

document.addEventListener('DOMContentLoaded', () => {
  // Login
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('loginEmail').value;
      const password = document.getElementById('loginPassword').value;
      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });
        if (res.ok) {
          const data = await res.json();
          localStorage.setItem('token', data.token);
          window.location.href = '/';
        } else {
          alert('Login failed. Check your credentials.');
        }
      } catch (err) {
        alert('Login error.');
      }
    });
  }

  // Register
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('registerEmail').value;
      const password = document.getElementById('registerPassword').value;
      try {
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });
        if (res.ok) {
          alert('Registration successful! Please log in.');
          window.location.href = '/login.html';
        } else {
          alert('Registration failed. Email may already be in use.');
        }
      } catch (err) {
        alert('Registration error.');
      }
    });
  }

  // Logout (if on logout page)
  if (window.location.pathname.endsWith('logout.html')) {
    fetch('/api/auth/logout', { method: 'POST', headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') } })
      .then(() => localStorage.removeItem('token'));
  }
});
