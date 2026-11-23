(() => {
    'use strict';
    const dialog = document.getElementById('rental-return-dialog');
    if (!dialog) return;
    const panel = document.getElementById('selected-order'),
        form = document.getElementById('rental-return-form'),
        actual = document.getElementById('return-actual-time'),
        dueTime = document.getElementById('return-due-time'),
        adjust = document.getElementById('return-adjust-fee'),
        adjusted = document.getElementById('return-adjusted-fee'),
        reason = document.getElementById('return-adjustment-reason'),
        collected = document.getElementById('return-fee-collected'),
        save = document.getElementById('save-rental-return');
    let dueDate = '',
        dailyRate = 0,
        trigger = null;
    const money = (value) => `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value)}đ`;
    function now() {
        const parts = Object.fromEntries(
            new Intl.DateTimeFormat('en-CA', {
                timeZone: 'Asia/Ho_Chi_Minh',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                hourCycle: 'h23',
            })
                .formatToParts(new Date())
                .filter((p) => p.type !== 'literal')
                .map((p) => [p.type, p.value]),
        );
        return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`;
    }
    const stamp = (value) => Date.parse(`${value.length === 16 ? value + ':00' : value}Z`);
    function preview() {
        const seconds = Math.max(0, Math.floor((stamp(actual.value) - stamp(`${dueDate}T${dueTime.value}`)) / 1000));
        const valid = Number.isFinite(seconds) && Number.isFinite(dailyRate) && dailyRate >= 0;
        if (!valid) {
            document.getElementById('return-delay').textContent = 'Chọn đủ ngày và giờ';
            document.getElementById('return-calculated-fee').textContent = '—';
            save.disabled = true;
            return;
        }
        save.disabled = false;
        const days = Math.floor(seconds / 86400),
            rest = seconds % 86400,
            extra = rest <= 3600 ? 0 : rest <= 10800 ? 10 : rest <= 21600 ? 20 : rest <= 43200 ? 40 : 70,
            fee = Math.round((dailyRate * (days * 70 + extra)) / 100);
        const hours = Math.floor(rest / 3600),
            minutes = Math.floor((rest % 3600) / 60);
        document.getElementById('return-delay').textContent =
            seconds === 0 ? 'Đúng hạn / trả sớm' : `${days ? days + ' ngày ' : ''}${hours} giờ ${minutes} phút`;
        document.getElementById('return-calculated-fee').textContent = money(fee);
        if (fee === 0) {
            adjust.checked = false;
            collected.checked = false;
        }
        adjust.disabled = fee === 0;
        document.getElementById('return-adjust-fields').hidden = !adjust.checked;
        adjusted.disabled = !adjust.checked;
        adjusted.required = adjust.checked;
        adjusted.max = String(fee);
        reason.disabled = !adjust.checked;
        if (adjust.checked && adjusted.value === '' && document.activeElement !== adjusted)
            adjusted.value = String(fee);
        const final = adjust.checked ? Number(adjusted.value) : fee;
        reason.required = adjust.checked && final < fee;
        collected.disabled = final <= 0;
        if (collected.disabled) collected.checked = false;
    }
    panel.addEventListener('click', (event) => {
        const button = event.target.closest('[data-open-return]');
        if (!button) return;
        trigger = button;
        form.reset();
        dueDate = button.dataset.returnDate;
        dailyRate = Number(button.dataset.dailyRate);
        document.getElementById('return-rental-id').value = button.dataset.openReturn;
        document.getElementById('return-order-code').textContent = `Đơn #ORD-${button.dataset.openReturn}`;
        document.getElementById('return-due-date').textContent = dueDate.split('-').reverse().join('/');
        document.getElementById('return-daily-rate').textContent = money(dailyRate);
        dueTime.value = button.dataset.returnTime || '17:00';
        dueTime.readOnly = Boolean(button.dataset.returnTime);
        document.getElementById('return-legacy-note').hidden = Boolean(button.dataset.returnTime);
        actual.value = now();
        actual.max = now();
        actual.min = `${button.dataset.returnStart}T00:00`;
        adjusted.value = '';
        reason.value = '';
        preview();
        dialog.showModal();
        actual.focus();
    });
    actual.addEventListener('focus', () => {
        actual.max = now();
    });
    [actual, dueTime, adjust, adjusted].forEach((field) => field.addEventListener('input', preview));
    [document.getElementById('close-rental-return'), document.getElementById('cancel-rental-return')].forEach(
        (button) => button.addEventListener('click', () => dialog.close()),
    );
    dialog.addEventListener('close', () => {
        if (trigger?.isConnected) trigger.focus();
        trigger = null;
    });
    dialog.addEventListener('click', (event) => {
        const box = dialog.getBoundingClientRect();
        if (
            event.target === dialog &&
            (event.clientX < box.left ||
                event.clientX > box.right ||
                event.clientY < box.top ||
                event.clientY > box.bottom)
        )
            dialog.close();
    });
    form.addEventListener('submit', (event) => {
        preview();
        if (!form.reportValidity()) {
            event.preventDefault();
            return;
        }
        save.disabled = true;
    });
    window.addEventListener('pageshow', () => {
        save.disabled = false;
    });
})();
