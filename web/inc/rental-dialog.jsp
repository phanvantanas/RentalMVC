<%@page pageEncoding="UTF-8"%>
<dialog id="rental-dialog" class="modal">
    <form method="dialog">
        <button class="close-dialog" aria-label="Đóng">×</button>
    </form>
    <span class="eyebrow">THÔNG
        TIN THUÊ THIẾT BỊ</span>
    <h2 id="rental-name"></h2>
    <p class="muted">Chi phí tham khảo cho lịch thuê đã chọn.</p>
    <dl class="quote">
        <div>
            <dt>Ngày nhận – trả</dt>
            <dd id="quote-dates">${dateLabel} · ${pickupTime} – ${returnTime}</dd>
        </div>
        <div>
            <dt>Thời gian thuê</dt>
            <dd id="quote-days">${rentalDays} ngày</dd>
        </div>
        <div>
            <dt>Giá thuê / ngày</dt>
            <dd id="quote-price"></dd>
        </div>
        <div>
            <dt>Tiền thuê trước ưu đãi</dt>
            <dd id="quote-subtotal"></dd>
        </div>
        <div>
            <dt>Ưu đãi thời gian thuê</dt>
            <dd id="quote-discount"></dd>
        </div>
        <div class="quote-total">
            <dt>Tổng tiền thuê</dt>
            <dd id="quote-total"></dd>
        </div>
        <div>
            <dt>Tiền đặt cọc</dt>
            <dd id="quote-deposit"></dd>
        </div>
    </dl>
    <p class="dialog-note">Thêm vào giỏ hàng hoặc thanh toán riêng 1 thiết bị theo lịch và số tiền ở trên.</p>
    <form class="quote-cart-form" action="${ctx}/cart" method="post">
        <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
        <input type="hidden" name="product" id="quote-product-id">
        <input type="hidden" name="start" value="${startDate}">
        <input type="hidden" name="end" value="${endDate}">
        <input type="hidden" name="pickup_time" value="${pickupTime}">
        <input type="hidden" name="return_time" value="${returnTime}">
        <button class="quote-add-cart" name="action" value="add" type="submit">＋ Thêm vào giỏ
            hàng</button>
        <button class="button quote-checkout" name="action" value="buy" type="submit">Thanh toán ngay
            <svg>
                <use href="#i-arrow" />
            </svg>
        </button>
    </form>
</dialog>
