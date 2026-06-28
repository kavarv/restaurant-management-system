/* login.js — Auth page helpers */
(function () {
  'use strict';

  /* Toggle password visibility */
  document.querySelectorAll('.toggle-password').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = this.previousElementSibling;
      if (!input || input.tagName !== 'INPUT') input = this.parentElement.querySelector('input');
      if (!input) return;
      if (input.type === 'password') { input.type = 'text'; this.textContent = '🙈'; }
      else                           { input.type = 'password'; this.textContent = '👁'; }
    });
  });

  /* Submit button loading state */
  document.querySelectorAll('form.login-form, form#registerForm').forEach(function (form) {
    form.addEventListener('submit', function () {
      var btn = form.querySelector('[type="submit"]');
      if (btn) { btn.disabled = true; btn.classList.add('loading'); }
    });
  });
})();
