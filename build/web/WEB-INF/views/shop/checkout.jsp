<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Thanh toán VietQR | LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/catalog.css?v=20261119-rating-right">
    <link rel="stylesheet" href="${ctx}/assets/css/shop.css?v=20261118-footer-bold">
    <script src="${ctx}/assets/vendor/qrcode-generator-2.0.4.js" defer></script>
    <script src="${ctx}/assets/js/shop.js?v=hold30" defer></script>
    <link rel="stylesheet" href="${ctx}/assets/css/rental-cancel.css?v=1">
</head>

<body>
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/shop/header.jsp"%>
    <main class="shop-container">
        <div class="shop-page-heading">
            <span class="eyebrow">SECURE RENTAL CHECKOUT</span>
            <h1>Thanh toán đơn thuê</h1>
            <p>Chuyển khoản đúng số tiền và nội dung để đơn được đối soát.</p>
        </div>
        <c:if test="${not empty checkoutError}">
            <p class="shop-error" role="alert">
                <c:out value="${checkoutError}" />
            </p>
        </c:if>
        <c:choose>
            <c:when test="${not empty payment}">
                <section class="payment-card">
                    <div class="payment-title">
                        <span class="payment-order">ĐƠN #ORD-${payment.rental_id}</span>
                        <span class="payment-state ${payment.status}">${payment.status eq 'paid' ? 'Đã nhận tiền' : (payment.status eq 'reported' ? 'Chờ đối soát' : 'Chờ chuyển khoản')}</span>
                    </div>
                    <c:choose>
                        <c:when test="${payment.rental_status eq 'cancelled'}">
                            <div class="shop-error">
                                <h2>Đơn thuê đã hủy hoặc hết hạn giữ máy</h2>
                                <p>Đơn chưa báo chuyển khoản chỉ được giữ máy trong 30 phút. Vui lòng không chuyển khoản
                                    cho đơn này. Nếu đã chuyển tiền, hãy liên hệ LensCam để được hỗ trợ.</p>
                            </div>
                        </c:when>
                        <c:when test="${payment.status eq 'paid'}">
                            <div class="payment-success">
                                <svg>
                                    <use href="#i-check" />
                                </svg>
                                <h2>Đã xác nhận nhận tiền</h2>
                                <p>Đơn thuê của bạn đã được ghi nhận thanh toán. LensCam sẽ xác nhận lịch bàn giao thiết
                                    bị.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:if test="${payment.status eq 'pending'}">
                                <p class="shop-error" role="status" id="hold-countdown" data-seconds="${payment.holdSeconds}">Máy
                                    được giữ trong 30 phút từ lúc tạo đơn. Thời gian còn lại: <strong id="hold-time">
                                    </strong>. Hãy chuyển khoản và báo đã chuyển trong thời hạn này.
                                </p>
                            </c:if>
                            <div class="payment-grid">
                                <div class="qr-panel">
                                    <img class="vietqr-image" data-payload="<c:out value='${payment.qrPayload}'/>"
                                        alt="VietQR thanh toán đơn ${payment.transfer_reference}">
                                    <p class="qr-fallback" hidden>Chưa tải được mã QR. Bạn có thể chuyển khoản thủ công
                                        theo thông tin bên cạnh.</p>
                                    <span>Quét mã bằng ứng dụng ngân hàng</span>
                                </div>
                                <div class="bank-details">
                                    <h2>Thông tin chuyển khoản</h2>
                                    <dl>
                                        <div>
                                            <dt>Ngân hàng</dt>
                                            <dd>
                                                <c:out value="${payment.bank_code}" />
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Chủ tài khoản</dt>
                                            <dd>
                                                <c:out value="${payment.account_holder}" />
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Số tài khoản</dt>
                                            <dd>
                                                <strong id="bank-account">
                                                    <c:out value="${payment.account_number}" />
                                                </strong>
                                                <button type="button" class="copy-button" data-copy="bank-account">Sao
                                                    chép</button>
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Số tiền</dt>
                                            <dd class="bank-amount">
                                                <fmt:formatNumber value="${payment.amount}" maxFractionDigits="0" />đ
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Nội dung</dt>
                                            <dd>
                                                <strong id="bank-reference">
                                                    <c:out value="${payment.transfer_reference}" />
                                                </strong>
                                                <button type="button" class="copy-button" data-copy="bank-reference">Sao
                                                    chép</button>
                                            </dd>
                                        </div>
                                    </dl>
                                    <p class="summary-note">Kiểm tra tên người nhận trên ứng dụng ngân hàng trước khi
                                        xác nhận chuyển tiền. Không thay đổi nội dung chuyển khoản.</p>
                                    <%@include file="/inc/shop/receipt-upload.jsp"%>
                                    <c:choose>
                                        <c:when test="${payment.status eq 'reported'}">
                                            <div class="payment-reported">Đã nhận thông báo chuyển khoản. Đơn đang chờ
                                                admin kiểm tra tiền thực nhận.</div>
                                        </c:when>
                                        <c:otherwise>
                                            <form action="${ctx}/checkout" method="post">
                                                <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
                                                <input type="hidden" name="order" value="${payment.rental_id}">
                                                <button class="button full-width" name="action" value="reported"
                                                    type="submit">Tôi đã chuyển khoản
                                                    <svg>
                                                        <use href="#i-check" />
                                                    </svg>
                                                </button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                    <p class="summary-note">Nút thông báo không tự xác nhận thanh toán. Admin sẽ đối
                                        soát sau khi tiền vào tài khoản.</p>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${payment.hasReceipt}">
                        <a class="receipt-view-link" href="${ctx}/payment/receipt?order=${payment.rental_id}"
                            target="_blank" rel="noopener">Xem
                            ảnh chuyển khoản đã gửi ↗</a>
                    </c:if>
                    <c:if test="${payment.status eq 'pending' and not payment.hasReceipt and (payment.rental_status eq 'pending' or payment.rental_status eq 'confirmed')}">
                        <c:set var="cancelOrderId" value="${payment.rental_id}" />
                        <%@include file="/inc/shop/cancel-order.jsp"%>
                    </c:if>
                    <div class="payment-bottom">
                        <a href="${ctx}/my-rentals?order=${payment.rental_id}">Theo dõi đơn thuê
                            →</a>
                        <a href="${ctx}/home">← Về trang chủ</a>
                        <a href="${ctx}/checkout?order=${payment.rental_id}">Cập nhật trạng thái ↻</a>
                    </div>
                </section>
            </c:when>
            <c:otherwise>
                <div class="shop-layout">
                    <section class="checkout-review">
                        <h2>Kiểm tra thông tin đơn thuê</h2>
                        <c:if test="${directCheckout}">
                            <p class="summary-note">Bạn đang thanh toán riêng thiết bị vừa chọn. Các thiết bị trong giỏ
                                hàng cũ được giữ nguyên.</p>
                        </c:if>
                        <dl class="checkout-customer">
                            <div>
                                <dt>Người thuê</dt>
                                <dd>
                                    <c:out value="${sessionScope.user.name}" />
                                </dd>
                            </div>
                            <div>
                                <dt>Số điện thoại</dt>
                                <dd>
                                    <c:out value="${sessionScope.user.phone}" />
                                </dd>
                            </div>
                            <div>
                                <dt>Email</dt>
                                <dd>
                                    <c:out value="${sessionScope.user.email}" />
                                </dd>
                            </div>
                        </dl>
                        <h3>Thiết bị trong đơn</h3>
                        <c:forEach items="${cartLines}" var="line">
                            <div class="checkout-line">
                                <span>
                                    <c:out value="${line.name}" />
                                    <small>Số lượng ${line.quantity} · ${cart.days}
                                        ngày</small>
                                </span>
                                <strong>
                                    <fmt:formatNumber value="${line.total}" maxFractionDigits="0" />đ
                                </strong>
                            </div>
                        </c:forEach>
                        <div class="payment-method">
                            <span class="method-dot"></span>
                            <div>
                                <strong>Chuyển khoản ngân hàng / VietQR</strong>
                                <p>Mã QR sẽ có sẵn số tiền và nội dung chuyển khoản. Máy được giữ trong 30 phút sau khi
                                    tạo đơn; hết hạn chưa báo chuyển khoản, đơn sẽ tự hủy.</p>
                            </div>
                            <span class="vietqr-tag">VietQR</span>
                        </div>
                        <p class="summary-note">Phí trả trễ: miễn tối đa 1 giờ; trên 1–3 giờ: 10%; trên 3–6 giờ: 20%;
                            trên 6–12 giờ: 40%; trên 12–24 giờ: 70% giá thuê/ngày ghi trong đơn. Mỗi 24 giờ trễ tính
                            70%, phần giờ dư áp dụng lại các mức trên.</p>
                        <a class="continue-shopping" href="${ctx}/${directCheckout ? 'home' : 'cart'}">${directCheckout ? '← Chọn lại thiết bị' : '← Chỉnh sửa giỏ hàng'}</a>
                    </section>
                    <aside class="shop-summary">
                        <%@include file="/inc/shop/summary.jsp"%>
                        <p class="summary-note">Giá trên chưa bao gồm tiền cọc. Thông tin đặt cọc được xác nhận khi nhận
                            thiết bị.</p>
                        <c:choose>
                            <c:when test="${canCheckout and bankReady}">
                                <form id="place-order-form" action="${ctx}/checkout" method="post">
                                    <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
                                    <input type="hidden" name="direct" value="${directCheckout ? '1' : '0'}">
                                    <input type="hidden" name="nonce" value="${checkoutNonce}">
                                    <button class="button full-width" type="submit">Tạo đơn
                                        &amp; lấy mã VietQR <svg>
                                            <use href="#i-arrow" />
                                        </svg>
                                    </button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <p class="shop-error">${not bankReady ? 'Đang cập nhật tài khoản nhận tiền. Vui lòng quay lại sau.' : 'Thiết bị hoặc lịch thuê không còn phù hợp. Vui lòng kiểm tra lại giỏ hàng.'}</p>
                                <a href="${ctx}/cart">Quay lại giỏ
                                    hàng</a>
                            </c:otherwise>
                        </c:choose>
                    </aside>
                </div>
            </c:otherwise>
        </c:choose>
    </main>
    <footer class="shop-footer">LensCam Rental Hub · 77 Nguyễn Huệ, Huế · 252 Nguyễn Tất Thành, Đà Nẵng</footer>
</body>

</html>
