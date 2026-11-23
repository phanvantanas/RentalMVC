const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
class Element {
    constructor() {
        this.handlers = {};
        this.children = [];
        this.dataset = {};
        this.hidden = false;
        this.value = '';
        this.textContent = '';
        this.attributes = {};
    }
    addEventListener(name, handler) {
        (this.handlers[name] ||= []).push(handler);
    }
    emit(name, extra = {}) {
        const event = {
            preventDefault() {
                this.prevented = true;
            },
            ...extra,
        };
        for (const fn of this.handlers[name] || []) fn(event);
        return event;
    }
    append(node) {
        this.children.push(node);
    }
    replaceChildren() {
        this.children = [];
    }
    setAttribute(name, value) {
        this.attributes[name] = value;
    }
    focus() {
        this.focused = true;
    }
    reportValidity() {
        return this.value.length <= 1200;
    }
    set innerHTML(value) {
        throw new Error('Unsafe HTML rendering');
    }
}
(async () => {
    const nodes = new Map();
    const get = (id) => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    const root = get('#lens-chat');
    root.dataset.endpoint = '/RentalCamera/chatbot';
    root.querySelector = get;
    const suggestion = new Element();
    suggestion.dataset.chatQuestion = 'Tư vấn máy mới';
    root.querySelectorAll = () => [suggestion];
    get('#lens-chat-panel').hidden = true;
    const calls = [];
    let errorStatus = 0;
    const fetch = async (url, options) => {
        calls.push({ url, options });
        const post = options.method === 'POST';
        const status = post ? errorStatus : 0;
        return {
            ok: !status,
            status: status || 200,
            headers: { get: () => 'application/json' },
            json: async () =>
                post
                    ? status
                        ? { error: 'Phiên đã thay đổi' }
                        : { reply: '<script>alert(1)</script> Canon: 150.000đ/ngày' }
                    : { ready: true, csrf: 'test-csrf', messages: [] },
        };
    };
    vm.runInNewContext(fs.readFileSync('web/assets/js/chatbot.js', 'utf8'), {
        document: { querySelector: get, createElement: () => new Element() },
        fetch,
        AbortSignal,
        URLSearchParams,
    });
    const settle = () => new Promise((resolve) => setImmediate(resolve));
    get('#lens-chat-launcher').emit('click');
    await settle();
    assert.equal(get('#lens-chat-panel').hidden, false);
    assert.equal(get('#lens-chat-launcher').attributes['aria-expanded'], 'true');
    assert.equal(calls.length, 1);
    suggestion.emit('click');
    await settle();
    assert.equal(calls[1].options.body.get('csrf'), 'test-csrf');
    assert.equal(calls[1].options.body.get('message'), 'Tư vấn máy mới');
    assert.equal(get('#lens-chat-messages').children[1].textContent, '<script>alert(1)</script> Canon: 150.000đ/ngày');
    assert.equal(get('#lens-chat-send').disabled, false);
    get('#lens-chat-input').value = 'Chưa gửi';
    get('#lens-chat-input').emit('keydown', { key: 'Enter', isComposing: true });
    await settle();
    assert.equal(calls.length, 2);
    root.emit('keydown', { key: 'Escape' });
    assert.equal(get('#lens-chat-panel').hidden, true);
    errorStatus = 403;
    get('#lens-chat-launcher').emit('click');
    get('#lens-chat-form').emit('submit');
    await settle();
    assert.equal(get('#lens-chat-status').textContent, 'Phiên đã thay đổi');
    assert.equal(get('#lens-chat-input').value, 'Chưa gửi');
    assert.equal(get('#lens-chat-send').disabled, true);
    get('#lens-chat-close').emit('click');
    get('#lens-chat-launcher').emit('click');
    await settle();
    assert.equal(calls.at(-1).options.method, undefined);
    console.log(
        'PASS: chatbot open/close, suggested questions, CSRF request, safe text rendering, IME input and expired-session recovery.',
    );
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
