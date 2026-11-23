(() => {
    'use strict';
    const root = document.querySelector('#lens-chat');
    if (!root) return;
    const launcher = root.querySelector('#lens-chat-launcher');
    const panel = root.querySelector('#lens-chat-panel');
    const close = root.querySelector('#lens-chat-close');
    const form = root.querySelector('#lens-chat-form');
    const input = root.querySelector('#lens-chat-input');
    const send = root.querySelector('#lens-chat-send');
    const log = root.querySelector('#lens-chat-messages');
    const status = root.querySelector('#lens-chat-status');
    const suggestions = root.querySelector('#lens-chat-suggestions');
    let csrf = '',
        busy = false,
        loading = null;
    function message(text, role) {
        const bubble = document.createElement('div');
        bubble.className = `lens-chat-message lens-chat-${role}`;
        bubble.textContent = text;
        log.append(bubble);
        log.scrollTop = log.scrollHeight;
    }
    async function jsonResponse(response) {
        const type = response.headers.get('content-type') || '';
        if (!type.includes('application/json'))
            throw new Error('Phiên trò chuyện đã thay đổi. Bạn tải lại trang rồi thử nhé.');
        return response.json();
    }
    async function initialize() {
        status.textContent = 'Đang kết nối trợ lý…';
        send.disabled = true;
        try {
            const response = await fetch(root.dataset.endpoint, {
                credentials: 'same-origin',
                cache: 'no-store',
                signal: AbortSignal.timeout(12000),
            });
            const data = await jsonResponse(response);
            if (!response.ok) throw new Error(data.error || 'Chưa kết nối được trợ lý.');
            csrf = data.csrf;
            if (data.messages?.length) {
                log.replaceChildren();
                for (const turn of data.messages) message(turn.text, turn.role === 'user' ? 'user' : 'assistant');
                suggestions.hidden = true;
            }
            status.textContent = data.ready ? '' : 'Trợ lý đang được cấu hình, vui lòng thử lại sau.';
        } catch (error) {
            csrf = '';
            status.textContent =
                error.name === 'TimeoutError' ? 'Kết nối chậm, bạn thử mở lại trợ lý nhé.' : error.message;
        } finally {
            send.disabled = !csrf || busy;
            loading = null;
        }
    }
    function show(open) {
        panel.hidden = !open;
        launcher.setAttribute('aria-expanded', String(open));
        if (open) {
            if (!csrf && !loading) loading = initialize();
            input.focus();
        } else launcher.focus();
    }
    launcher.addEventListener('click', () => show(panel.hidden));
    close.addEventListener('click', () => show(false));
    root.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !panel.hidden) show(false);
    });
    async function submit() {
        const text = input.value.trim();
        if (busy || !text || !csrf) return;
        if (!input.reportValidity()) return;
        busy = true;
        send.disabled = true;
        suggestions.hidden = true;
        message(text, 'user');
        input.value = '';
        status.textContent = 'LensCam đang trả lời…';
        try {
            const response = await fetch(root.dataset.endpoint, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: new URLSearchParams({ csrf, message: text }),
                signal: AbortSignal.timeout(45000),
            });
            const data = await jsonResponse(response);
            if (response.status === 403) csrf = '';
            if (!response.ok) throw new Error(data.error || 'Chưa gửi được câu hỏi.');
            message(data.reply, 'assistant');
            status.textContent = '';
        } catch (error) {
            status.textContent =
                error.name === 'TimeoutError' ? 'Phản hồi lâu hơn dự kiến. Bạn thử lại sau nhé.' : error.message;
            input.value = text;
        } finally {
            busy = false;
            send.disabled = !csrf;
            if (!panel.hidden) input.focus();
        }
    }
    form.addEventListener('submit', (event) => {
        event.preventDefault();
        submit();
    });
    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            submit();
        }
    });
    root.querySelectorAll('[data-chat-question]').forEach((button) =>
        button.addEventListener('click', () => {
            input.value = button.dataset.chatQuestion;
            submit();
        }),
    );
})();
