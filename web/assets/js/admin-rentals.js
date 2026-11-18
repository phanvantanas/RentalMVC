(() => {
    'use strict';
    const rows = [...document.querySelectorAll('.order-row')];
    const search = document.querySelector('#order-search');
    const from = document.querySelector('#filter-from');
    const to = document.querySelector('#filter-to');
    const panel = document.querySelector('#selected-order');
    const pageSize = 8;
    let status = '', payment = '', page = 1, selected = null, filtered = [];
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
            && (!payment || row.dataset.payment === payment && row.dataset.status !== 'cancelled')
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
        search.value = ''; from.value = ''; to.value = ''; status = ''; payment = ''; document.querySelector('#payment-filter-note').hidden = true; filter();
    });
    document.querySelector('#previous-page').addEventListener('click', () => { page--; render(); });
    document.querySelector('#next-page').addEventListener('click', () => { page++; render(); });
    document.querySelector('#refresh-orders').addEventListener('click', () => location.reload());
    document.querySelectorAll('[data-detail]').forEach(button => button.addEventListener('click', () => {
        showDetail(button.dataset.detail);
        if (matchMedia('(max-width: 680px)').matches) panel.scrollIntoView({behavior:'smooth', block:'start'});
    }));
    const confirmation = document.getElementById('rental-confirm-dialog');
    const confirmButton = document.getElementById('rental-confirm-submit');
    let pendingAction = null, approvedForm = null;
    panel.addEventListener('submit', event => {
        const form = event.target;
        if (!form.matches('.status-action-form')) return;
        if (form.dataset.submitting === 'true') { event.preventDefault(); return; }
        if (approvedForm === form) { approvedForm = null; form.dataset.submitting = 'true'; return; }
        event.preventDefault();
        if (confirmation.open) return;
        const button = event.submitter || form.querySelector('button[type="submit"]');
        if (!button) return;
        pendingAction = { form, button };
        const isCancellation = button.value === 'cancelled';
        const isPayment = form.elements.namedItem('action')?.value === 'confirm_payment';
        const descriptions = {confirmed:'Đơn thuê sẽ được duyệt và chuyển sang chờ giao máy.',renting:'Xác nhận bạn đã giao đủ thiết bị cho khách. Đơn sẽ chuyển sang đang thuê.',returned:'Xác nhận bạn đã nhận lại và kiểm tra thiết bị. Đơn sẽ chuyển sang đã trả.',cancelled:'Đơn thuê sẽ được chuyển sang đã hủy.'};
        document.getElementById('rental-confirm-title').textContent = `${button.textContent.trim()}?`;
        document.getElementById('rental-confirm-order').textContent = `Đơn #ORD-${form.elements.namedItem('id').value}`;
        document.getElementById('rental-confirm-description').textContent = isPayment ? 'Chỉ xác nhận sau khi đã đối chiếu và nhận đủ tiền trong tài khoản ngân hàng.' : (descriptions[button.value] || 'Thao tác sẽ cập nhật trạng thái đơn thuê.');
        confirmation.classList.toggle('is-cancellation', isCancellation);
        confirmButton.textContent = isCancellation ? 'Xác nhận hủy đơn' : 'Xác nhận';
        confirmButton.disabled = false;
        confirmation.showModal();
    });
    confirmation.querySelectorAll('[data-cancel-rental-confirm]').forEach(button => button.addEventListener('click', () => confirmation.close()));
    confirmation.addEventListener('close', () => { const trigger = pendingAction?.button; pendingAction = null; if (trigger?.isConnected) trigger.focus(); });
    confirmation.addEventListener('click', event => { const box = confirmation.getBoundingClientRect(); if (event.target === confirmation && (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom)) confirmation.close(); });
    confirmButton.addEventListener('click', () => {
        if (!pendingAction) return;
        const action = pendingAction; pendingAction = null;
        if (!action.form.isConnected || !action.button.isConnected) { confirmation.close(); return; }
        confirmButton.disabled = true;
        approvedForm = action.form;
        confirmation.close();
        action.form.requestSubmit(action.button);
        approvedForm = null;
    });
    window.addEventListener('pageshow', () => { panel.querySelectorAll('.status-action-form').forEach(form => { delete form.dataset.submitting; }); });
    const query = new URLSearchParams(location.search);
    if (['pending','confirmed','renting','returned','cancelled','overdue'].includes(query.get('status'))) status = query.get('status');
    if (query.get('payment') === 'reported') payment = 'reported';
    document.querySelector('#payment-filter-note').hidden = !payment;
    document.querySelector('#clear-payment-filter').addEventListener('click',()=>{payment='';document.querySelector('#payment-filter-note').hidden=true;filter();});
    const orderId = query.get("order");
    if (orderId && /^\d+$/.test(orderId)) search.value = `ORD-${orderId}`;
    filter();
})();
