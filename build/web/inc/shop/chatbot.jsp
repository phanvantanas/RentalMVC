<%@page pageEncoding="UTF-8"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/chatbot.css?v=1">
<div class="lens-chat" id="lens-chat" data-endpoint="${pageContext.request.contextPath}/chatbot">
    <button type="button" class="lens-chat-launcher" id="lens-chat-launcher" aria-label="Mở trợ lý tư vấn thuê máy"
        aria-expanded="false" aria-controls="lens-chat-panel">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round" aria-hidden="true">
            <path
                d="M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6A8.4 8.4 0 0 1 12.5 3h.5a8.5 8.5 0 0 1 8 8v.5Z" />
            <path d="M8 12h.01M12 12h.01M16 12h.01" />
        </svg>
        <span>Tư vấn thuê máy</span>
    </button>
    <section class="lens-chat-panel" id="lens-chat-panel" aria-labelledby="lens-chat-title" hidden>
        <header class="lens-chat-heading">
            <span class="lens-chat-avatar" aria-hidden="true">L</span>
            <div>
                <h2 id="lens-chat-title">LensCam tư vấn</h2>
                <p>Trợ lý AI · Đồng hành cùng buổi chụp</p>
            </div>
            <button type="button" id="lens-chat-close" aria-label="Đóng trợ lý">×</button>
        </header>
        <div class="lens-chat-messages" id="lens-chat-messages" role="log" aria-live="polite" aria-relevant="additions">
            <div class="lens-chat-message lens-chat-assistant">Chào bạn! Mình có thể giúp chọn máy, tham khảo giá thuê
                và giải thích cách đặt lịch. Bạn muốn chụp hay quay gì?</div>
        </div>
        <div class="lens-chat-suggestions" id="lens-chat-suggestions">
            <button type="button" data-chat-question="Mình mới chụp ảnh, nên thuê máy nào?">Máy cho người mới</button>
            <button type="button" data-chat-question="Thanh toán và tiền cọc như thế nào?">Thanh toán &amp;
                cọc</button>
            <button type="button" data-chat-question="Thuê nhiều ngày được giảm bao nhiêu phần trăm?">Ưu
                đãi theo ngày</button>
        </div>
        <p class="lens-chat-status" id="lens-chat-status" role="status"></p>
        <form class="lens-chat-form" id="lens-chat-form">
            <label class="lens-chat-sr" for="lens-chat-input">Câu hỏi tư
                vấn thuê máy</label>
            <textarea id="lens-chat-input" rows="2" maxlength="1200" placeholder="Bạn cần tư vấn điều gì?"
                required></textarea>
            <button type="submit" id="lens-chat-send" aria-label="Gửi câu hỏi">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                    stroke-linejoin="round" aria-hidden="true">
                    <path d="m22 2-7 20-4-9-9-4L22 2ZM22 2 11 13" />
                </svg>
            </button>
        </form>
        <p class="lens-chat-disclaimer">AI hỗ trợ tham khảo. Lịch trống và giá được xác nhận khi đặt thuê.</p>
    </section>
</div>
<script src="${pageContext.request.contextPath}/assets/js/chatbot.js?v=1" defer></script>
