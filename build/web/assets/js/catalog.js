(() => {
    'use strict';
    const grid = document.querySelector('#product-grid');
    if (!grid) return;
    const cards = [...grid.querySelectorAll('.product-card')];
    const category = document.querySelector('#category-select');
    const device = document.querySelector('#device-select');
    const searchForm = document.querySelector('#search-form');
    const available = document.querySelector('#available-only');
    const categoryFilter = document.querySelector('#category-filter');
    const categoryFilterToggle = categoryFilter.querySelector('summary');
    let sortMode = 'brand';
    const start = document.querySelector('#start-date');
    const end = document.querySelector('#end-date');
    const daysLabel = document.querySelector('#rental-days');
    const pickup = document.querySelector('#pickup-time');
    const dropoff = document.querySelector('#return-time');
    const quoteForm = document.querySelector('.quote-cart-form');
    const discountForDays = (days) => (days >= 7 ? 20 : days >= 5 ? 15 : days >= 3 ? 10 : 0);
    const moneyFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
    function refreshDevices(selected = '') {
        device.replaceChildren(
            new Option(category.value ? 'Tất cả thiết bị trong danh mục' : 'Chọn loại thiết bị trước...', ''),
        );
        device.disabled = !category.value;
        if (category.value)
            cards
                .filter((card) => card.dataset.category === category.value)
                .forEach((card) => {
                    device.add(new Option(card.dataset.name, card.dataset.id));
                });
        device.value = selected;
        if (device.selectedIndex < 0) device.value = '';
    }
    const knownBrands = ['Sony', 'Canon', 'Fujifilm', 'DJI', 'Nikon', 'Panasonic', 'Sigma', 'Tamron', 'Godox'];
    cards.forEach((card) => {
        const name = card.dataset.name;
        const brand = knownBrands.find((value) => name.toLowerCase().includes(value.toLowerCase())) || 'Khác';
        card.dataset.brand = brand;
    });
    function applyFilters() {
        let count = 0;
        cards.forEach((card) => {
            const visible =
                (!category.value || card.dataset.category === category.value) &&
                (!device.value || card.dataset.id === device.value) &&
                (!available.checked || Number(card.dataset.stock) > 0);
            card.hidden = !visible;
            if (visible) count++;
        });
        document.querySelector('#results-count').textContent =
            `${count} thiết bị${available.checked ? ' còn trống' : ''}`;
        document.querySelector('#empty-state').hidden = count > 0;
        document.querySelectorAll('.category-tab').forEach((tab) => {
            const active = tab.dataset.category === category.value;
            tab.classList.toggle('active', active);
            tab.setAttribute('aria-pressed', active);
        });
        document.querySelector('#category-filter-selection').textContent = category.value
            ? category.selectedOptions[0].textContent.trim()
            : 'Tất cả hãng';
        document.querySelectorAll('[data-sort]').forEach((button) => {
            const active = button.dataset.sort === sortMode;
            button.classList.toggle('active', active);
            button.setAttribute('aria-pressed', active);
        });
        const sorted = [...cards].sort((a, b) => {
            if (sortMode === 'price-asc') return Number(a.dataset.price) - Number(b.dataset.price);
            if (sortMode === 'price-desc') return Number(b.dataset.price) - Number(a.dataset.price);
            if (sortMode === 'name') return a.dataset.name.localeCompare(b.dataset.name, 'vi');
            if (sortMode === 'brand')
                return (
                    a.dataset.brand.localeCompare(b.dataset.brand, 'vi') ||
                    a.dataset.name.localeCompare(b.dataset.name, 'vi')
                );
            return Number(a.dataset.order) - Number(b.dataset.order);
        });
        sorted.forEach((card) => grid.append(card));
    }
    function resetFilters() {
        category.value = '';
        refreshDevices();
        available.checked = false;
        sortMode = 'brand';
        applyFilters();
    }
    document.querySelector('#empty-reset').addEventListener('click', resetFilters);
    category.addEventListener('change', () => {
        refreshDevices();
        applyFilters();
    });
    [device, available].forEach((element) => element.addEventListener('change', applyFilters));
    document.querySelectorAll('[data-sort]').forEach((button) =>
        button.addEventListener('click', () => {
            sortMode = button.dataset.sort;
            applyFilters();
        }),
    );
    document.querySelectorAll('.category-tab').forEach((tab) =>
        tab.addEventListener('click', () => {
            category.value = tab.dataset.category;
            refreshDevices();
            applyFilters();
            categoryFilter.open = false;
            categoryFilterToggle.focus();
        }),
    );
    document.addEventListener('click', (event) => {
        if (!categoryFilter.contains(event.target)) categoryFilter.open = false;
    });
    categoryFilter.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            categoryFilter.open = false;
            categoryFilterToggle.focus();
        }
    });
    document.querySelectorAll('[data-view]').forEach((button) =>
        button.addEventListener('click', () => {
            grid.classList.toggle('list-view', button.dataset.view === 'list');
            document.querySelectorAll('[data-view]').forEach((item) => {
                item.classList.toggle('active', item === button);
                item.setAttribute('aria-pressed', item === button);
            });
        }),
    );
    function validateDates() {
        end.setCustomValidity('');
        const from = new Date(`${start.value}T00:00:00Z`);
        const to = new Date(`${end.value}T00:00:00Z`);
        const days = Math.round((to - from) / 86400000);
        if (Number.isFinite(days) && days > 0 && days <= 365) {
            const discount = discountForDays(days);
            daysLabel.textContent = `${days} ngày${discount ? ` · Ưu đãi ${discount}%` : ''}`;
            document
                .querySelectorAll('.offer-tier')
                .forEach((tier) =>
                    tier.classList.toggle(
                        'selected',
                        Number(tier.dataset.minDays) === (days >= 7 ? 7 : days >= 5 ? 5 : days >= 3 ? 3 : 0),
                    ),
                );
        } else {
            daysLabel.textContent = 'Chọn ngày trả';
            document.querySelectorAll('.offer-tier').forEach((tier) => tier.classList.remove('selected'));
            end.setCustomValidity('Ngày trả phải sau ngày nhận, thời gian thuê tối đa 365 ngày.');
        }
        return days;
    }
    start.addEventListener('change', validateDates);
    end.addEventListener('change', validateDates);
    searchForm.addEventListener('submit', (event) => {
        validateDates();
        if (!event.currentTarget.reportValidity()) event.preventDefault();
        if (!available.checked) {
            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'available';
            hidden.value = '0';
            event.currentTarget.append(hidden);
        }
    });
    document.addEventListener('keydown', (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            start.scrollIntoView({ block: 'center' });
            start.focus();
        }
    });
    document
        .querySelectorAll('[data-open]')
        .forEach((button) =>
            button.addEventListener('click', () => document.getElementById(button.dataset.open).showModal()),
        );
    document.querySelectorAll('dialog').forEach((dialog) =>
        dialog.addEventListener('click', (event) => {
            const bounds = dialog.getBoundingClientRect();
            if (
                event.target === dialog &&
                (event.clientX < bounds.left ||
                    event.clientX > bounds.right ||
                    event.clientY < bounds.top ||
                    event.clientY > bounds.bottom)
            )
                dialog.close();
        }),
    );
    const currency = (value) => `${moneyFormat.format(value)}đ`;
    function syncQuote() {
        const days = validateDates();
        if (!searchForm.reportValidity()) return false;
        const discount = discountForDays(days);
        for (const [name, value] of Object.entries({
            start: start.value,
            end: end.value,
            pickup_time: pickup.value,
            return_time: dropoff.value,
        })) {
            quoteForm.elements.namedItem(name).value = value;
        }
        const dateLabel = (value) => value.split('-').reverse().join('/');
        document.querySelector('#quote-dates').textContent =
            `${dateLabel(start.value)} – ${dateLabel(end.value)} · ${pickup.value} – ${dropoff.value}`;
        document.querySelector('#quote-days').textContent = `${days} ngày`;
        const subtotal = Number(quoteForm.dataset.price) * days;
        const saving = Math.round((subtotal * discount) / 100);
        document.querySelector('#quote-subtotal').textContent = currency(subtotal);
        document.querySelector('#quote-discount').textContent = `${discount}% (−${currency(saving)})`;
        document.querySelector('#quote-total').textContent = currency(subtotal - saving);
        return true;
    }
    quoteForm.addEventListener('submit', (event) => {
        if (!syncQuote()) event.preventDefault();
    });
    document.querySelectorAll('.rent-button:not(:disabled)').forEach((button) =>
        button.addEventListener('click', () => {
            const card = button.closest('.product-card');
            quoteForm.dataset.price = card.dataset.price;
            if (!syncQuote()) return;
            document.querySelector('#rental-name').textContent = card.dataset.name;
            document.querySelector('#quote-product-id').value = card.dataset.id;
            document.querySelector('#quote-price').textContent = currency(Number(card.dataset.price));
            document.querySelector('#quote-deposit').textContent =
                card.dataset.depositKnown === 'true' ? currency(Number(card.dataset.deposit)) : 'Liên hệ để xác nhận';
            document.querySelector('#rental-dialog').showModal();
        }),
    );
    document.querySelectorAll('.product-visual img').forEach((img) => {
        const fallback = () => {
            if (img.classList.contains('image-failed')) return;
            img.classList.add('image-failed');
            const placeholder = document.createElement('div');
            placeholder.className = 'image-placeholder';
            placeholder.textContent = 'Đang cập nhật ảnh';
            img.after(placeholder);
        };
        img.addEventListener('error', fallback, { once: true });
        if (img.complete && img.naturalWidth === 0) fallback();
    });
    refreshDevices(searchForm.dataset.product);
    validateDates();
    applyFilters();
})();
