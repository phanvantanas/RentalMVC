<%@page pageEncoding="UTF-8"%>
<section class="catalog-results" aria-label="Danh sách thiết bị">
    <div class="results-toolbar">
        <div>
            <h2 id="results-count" aria-live="polite">${fn:length(lstProduct)} thiết bị</h2>
            <span class="date-badge">${dateLabel}</span>
        </div>
    </div>
    <div class="product-grid" id="product-grid">
        <c:forEach items="${lstProduct}" var="prod" varStatus="loop">
            <c:set var="stock" value="${empty availableCounts[prod.id] ? 0 : availableCounts[prod.id]}" />
            <%@include file="/inc/product-card.jsp"%>
        </c:forEach>
    </div>
    <div class="empty-state" id="empty-state" ${empty lstProduct ? '' : 'hidden'}>
        <svg>
            <use href="#i-camera" />
        </svg>
        <h3>Chưa tìm thấy thiết bị phù hợp</h3>
        <p>Thử đổi ngày thuê hoặc mở rộng bộ lọc để xem thêm thiết bị.</p>
        <button type="button" class="button" id="empty-reset">Xóa bộ lọc</button>
    </div>
    <p class="catalog-note">Giá hiển thị theo ngày · Tiền đặt cọc được niêm yết riêng cho từng thiết bị.</p>
</section>
