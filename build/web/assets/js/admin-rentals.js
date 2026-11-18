(() => {
    'use strict';
    const rows = [...document.querySelectorAll('.order-row')];
    const search = document.querySelector('#order-search');
    const from = document.querySelector('#filter-from');
    const to = document.querySelector('#filter-to');
    const panel = document.querySelector('#selected-order');
    const pageSize = 8;
    let status = '', page = 1, selected = null, filtered = [];
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').toLowerCase();
    const searchTexts = new Map(rows.map(row => [row, normalize(row.textContent)]));
    function showDetail(id) {
        const template = document.getElementById(`order-detail-${id}`);
        if (!template) return;
        selected = String(id);
        panel.replaceChildren(template.content.cloneNode(true));
        rows.forEach(row => row.classList.toggle('selected', row.dataset.id === selected));
    }
    function render() {
        const maxPage = Math.max(1, Math.ceil(filtered.length / pageSize));
        page = Math.min(Math.max(1, page), maxPage);
        const slice = filtered.slice((page - 1) * pageSize, page * pageSize);
        rows.forEach(row => { row.hidden = !slice.includes(row); });
        document.querySelector('#orders-empty').hidden = filtered.length > 0;
        document.querySelector('#page-label').textContent = filtered.length ? `Hiển thị ${(page - 1) * pageSize + 1}–${Math.min(page * pageSize, filtered.length)} của ${filtered.length} đơn` : '0 đơn thuê';
        document.querySelector('#page-number').textContent = `${page} / ${maxPage}`;
        document.querySelector('#previous-page').disabled = page === 1;
        document.querySelector('#next-page').disabled = page === maxPage;
        if (!slice.some(row => row.dataset.id === selected)) {
            if (slice.length) showDetail(slice[0].dataset.id);
            else { selected = null; panel.textContent = 'Chọn một đơn trong danh sách để xem chi tiết.'; }
        }
    }
    function filter() {
        const term = normalize(search.value.trim());
        to.setCustomValidity(from.value && to.value && to.value < from.value ? 'Ngày kết thúc phải từ ngày bắt đầu trở đi.' : '');
        filtered = rows.filter(row => (!status || (status === 'overdue' ? row.dataset.overdue === 'true' : row.dataset.status === status))
            && searchTexts.get(row).includes(term)
            && (!from.value || row.dataset.to >= from.value)
            && (!to.value || row.dataset.from <= to.value));
        page = 1;
        document.querySelectorAll('[data-status]').forEach(button => {
            if (!button.matches('button')) return;
            const active = button.dataset.status === status;
            button.classList.toggle('active', active);
            button.setAttribute('aria-pressed', String(active));
        });
        render();
    }
    search.addEventListener('input', filter);
    from.addEventListener('change', filter);
    to.addEventListener('change', () => { filter(); to.reportValidity(); });
    document.querySelectorAll('button[data-status],button[data-quick-status]').forEach(button => button.addEventListener('click', () => {
        status = button.dataset.quickStatus || button.dataset.status || '';
        filter();
    }));
    document.querySelector('#clear-search').addEventListener('click', () => {
        search.value = ''; from.value = ''; to.value = ''; status = ''; filter();
    });
    document.querySelector('#previous-page').addEventListener('click', () => { page--; render(); });
    document.querySelector('#next-page').addEventListener('click', () => { page++; render(); });
    document.querySelector('#refresh-orders').addEventListener('click', () => location.reload());
    document.querySelectorAll('[data-detail]').forEach(button => button.addEventListener('click', () => {
        showDetail(button.dataset.detail);
        if (matchMedia('(max-width: 680px)').matches) panel.scrollIntoView({behavior:'smooth', block:'start'});
    }));
    panel.addEventListener('submit', event => {
        if (!event.target.matches('.status-action-form')) return;
        const button = event.submitter;
        const label = button ? button.textContent.trim() : 'Cập nhật đơn';
        if (!window.confirm(`${label}? Thao tác sẽ cập nhật trạng thái đơn thuê.`)) event.preventDefault();
    });
    const orderId = new URLSearchParams(location.search).get("order");
    if (orderId && /^\d+$/.test(orderId)) search.value = `ORD-${orderId}`;
    filter();
})();
