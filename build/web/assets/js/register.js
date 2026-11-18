(() => {
    'use strict';
    const form = document.querySelector('#register-form');
    if (!form) return;
    const password = document.querySelector('#register-password');
    const confirm = document.querySelector('#register-confirm');
    const toggle = document.querySelector('#register-toggle');
    const meter = document.querySelector('.password-strength');
    const strengthLabel = document.querySelector('#password-strength-label');
    const validateConfirm = () => confirm.setCustomValidity(confirm.value && confirm.value !== password.value ? 'Mật khẩu nhập lại chưa trùng khớp.' : '');
    password.addEventListener('input', () => {
        const value = password.value;
        const score = !value ? 0 : value.length < 8 ? 1 : Math.min(4, 2 + Number(/[A-Z]/.test(value) && /[a-z]/.test(value)) + Number(/[0-9]/.test(value) && /[^a-zA-Z0-9]/.test(value)));
        meter.dataset.level = String(score);
        strengthLabel.textContent = ['', 'Cần ít nhất 8 ký tự', 'Độ mạnh: Cơ bản', 'Độ mạnh: Khá', 'Độ mạnh: Tốt'][score];
        validateConfirm();
    });
    confirm.addEventListener('input', validateConfirm);
    toggle.addEventListener('click', () => {
        const show = password.type === 'password';
        password.type = show ? 'text' : 'password';
        toggle.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
        toggle.setAttribute('aria-pressed', String(show));
    });
    form.addEventListener('submit', event => {
        ['name', 'email', 'phone'].forEach(name => { const input = form.elements.namedItem(name); input.value = input.value.trim(); });
        validateConfirm();
        if (!form.reportValidity()) { event.preventDefault(); return; }
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = true;
        submit.querySelector('span').textContent = 'Đang tạo tài khoản…';
    });
    window.addEventListener('pageshow', () => {
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = false;
        submit.querySelector('span').textContent = 'Đăng ký tài khoản';
    });
})();
