(() => {
    'use strict';
    const form = document.querySelector('#login-form');
    if (!form) return;
    const password = document.querySelector('#password');
    const toggle = document.querySelector('#toggle-password');
    const identifier = document.querySelector('#emailphone');
    const remember = document.querySelector('#remember-account');
    const hint = document.querySelector('#remember-hint');
    const storageKey = 'lenscam.rememberedIdentifier';
    toggle.addEventListener('click', () => {
        const show = password.type === 'password';
        password.type = show ? 'text' : 'password';
        toggle.setAttribute('aria-pressed', String(show));
        toggle.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
    try {
        const saved = localStorage.getItem(storageKey);
        if (saved) { identifier.value = saved; remember.checked = true; hint.hidden = false; }
    } catch (_) { /* Login remains available when browser storage is blocked. */ }
    remember.addEventListener('change', () => {
        hint.hidden = !remember.checked;
        if (!remember.checked) {
            try { localStorage.removeItem(storageKey); } catch (_) { /* Optional browser preference. */ }
        }
    });
    form.addEventListener('submit', event => {
        identifier.value = identifier.value.trim();
        if (!form.reportValidity()) { event.preventDefault(); return; }
        try {
            if (remember.checked) localStorage.setItem(storageKey, identifier.value);
            else localStorage.removeItem(storageKey);
        } catch (_) { /* Storage is optional; never persist the password. */ }
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = true;
        submit.querySelector('span').textContent = 'Đang đăng nhập…';
    });
    window.addEventListener('pageshow', () => {
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = false;
        submit.querySelector('span').textContent = 'Đăng nhập';
    });
    document.querySelectorAll('[data-auth-dialog]').forEach(button => button.addEventListener('click', () => {
        document.getElementById(button.dataset.authDialog).showModal();
    }));
    document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('click', event => {
        const rect = dialog.getBoundingClientRect();
        if (event.target === dialog && (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom)) dialog.close();
    }));
})();
