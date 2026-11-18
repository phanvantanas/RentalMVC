'use strict';
document.getElementById('refresh-dashboard')?.addEventListener('click', () => window.location.reload());
document.getElementById('custom-toggle')?.addEventListener('click', function () {
    const range = document.getElementById('custom-range');
    range.hidden = !range.hidden;
    this.setAttribute('aria-expanded', String(!range.hidden));
    if (!range.hidden) range.querySelector('input').focus();
});
