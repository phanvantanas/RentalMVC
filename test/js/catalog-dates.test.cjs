const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

// Exercise the actual catalog event handlers without a browser or database.
class Element {
    constructor() {
        this.value = '';
        this.dataset = {};
        this.handlers = {};
        this.textContent = '';
        this.classList = {
            toggle() {},
            add() {},
            remove() {},
            contains() {
                return false;
            },
        };
    }
    addEventListener(name, handler) {
        (this.handlers[name] ||= []).push(handler);
    }
    emit(name) {
        const event = {
            currentTarget: this,
            prevented: false,
            preventDefault() {
                this.prevented = true;
            },
        };
        for (const handler of this.handlers[name] || []) handler(event);
        return event;
    }
    setAttribute() {}
    setCustomValidity(message) {
        this.error = message;
    }
    replaceChildren() {}
    append() {}
    querySelector() {
        return new Element();
    }
    showModal() {
        this.open = true;
    }
}
const elements = new Map();
const get = (selector) => {
    if (!elements.has(selector)) elements.set(selector, new Element());
    return elements.get(selector);
};
const card = new Element();
card.dataset = { id: '12', name: 'Sony camera', price: '100000', stock: '1' };
const button = new Element();
button.closest = () => card;
get('#product-grid').querySelectorAll = () => [card];
get('#search-form').dataset = { days: '3', product: '' };
get('#start-date').value = '2027-01-01';
get('#end-date').value = '2027-01-04';
get('#pickup-time').value = '08:00';
get('#return-time').value = '17:00';
get('#available-only').checked = true;
get('#search-form').reportValidity = () =>
    !get('#end-date').error && !!get('#start-date').value && !!get('#end-date').value;
const fields = Object.fromEntries(['start', 'end', 'pickup_time', 'return_time'].map((name) => [name, new Element()]));
get('.quote-cart-form').elements = { namedItem: (name) => fields[name] };
const document = {
    querySelector: get,
    querySelectorAll: (selector) => (selector === '.rent-button:not(:disabled)' ? [button] : []),
    addEventListener() {},
};
vm.runInNewContext(fs.readFileSync('web/assets/js/catalog.js', 'utf8'), {
    document,
    Intl,
    Date,
    Option: function () {},
    console,
});
const money = (amount) => `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(amount)}đ`;
for (const [days, total] of [
    [1, 100000],
    [2, 200000],
    [5, 425000],
    [7, 560000],
]) {
    get('#end-date').value = `2027-01-${String(1 + days).padStart(2, '0')}`;
    get('#end-date').emit('change');
    button.emit('click');
    assert.equal(get('#quote-days').textContent, `${days} ngày`);
    assert.equal(get('#quote-total').textContent, money(total));
    assert.equal(fields.end.value, get('#end-date').value);
    assert.equal(fields.start.value, get('#start-date').value);
}
get('#pickup-time').value = '10:00';
get('#return-time').value = '18:00';
get('.quote-cart-form').emit('submit');
assert.equal(fields.pickup_time.value, '10:00');
assert.equal(fields.return_time.value, '18:00');
assert.match(get('#quote-dates').textContent, /10:00 – 18:00/);
// A change since opening the quote must also reach both Add and Checkout submissions.
get('#end-date').value = '2027-01-03';
assert.equal(get('.quote-cart-form').emit('submit').prevented, false);
assert.equal(fields.end.value, '2027-01-03');
assert.equal(get('#quote-total').textContent, money(200000));
get('#end-date').value = '2027-01-01';
assert.equal(get('.quote-cart-form').emit('submit').prevented, true);
get('#rental-dialog').open = false;
button.emit('click');
assert.equal(get('#rental-dialog').open, false);
get('#end-date').value = '2028-01-03';
assert.equal(get('.quote-cart-form').emit('submit').prevented, true);
console.log('PASS: selected dates, discounts, pickup/return times, submission sync and invalid dates.');
