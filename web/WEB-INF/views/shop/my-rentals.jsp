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
    <title>Đơn thuê của tôi | LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/catalog.css?v=20261119-rating-right">
    <link rel="stylesheet" href="${ctx}/assets/css/shop.css?v=20261118-footer-bold">
    <link rel="stylesheet" href="${ctx}/assets/css/my-rentals.css?v=2-late-fee">
    <script src="${ctx}/assets/js/my-rentals.js" defer></script>
    <link rel="stylesheet" href="${ctx}/assets/css/rental-cancel.css?v=1">
</head>

<body>
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/shop/header.jsp"%>
    <main class="shop-container my-rentals">
        <div class="my-rentals-heading">
            <div>
                <span class="eyebrow">YOUR RENTAL JOURNEY</span>
                <h1>Đơn thuê của tôi</h1>
                <p>Theo dõi lịch nhận máy, thanh toán và tiến trình đơn thuê.</p>
            </div>
            <button class="button my-refresh" id="refresh-my-rentals" type="button">↻ Cập nhật trạng thái</button>
        </div>
        <c:if test="${not empty rentalFlash}">
            <p class="rental-flash" role="status">
                <c:out value="${rentalFlash}" />
            </p>
        </c:if>
        <c:if test="${not empty rentalFlashError}">
            <p class="shop-error" role="alert">
                <c:out value="${rentalFlashError}" />
            </p>
        </c:if>
        <c:choose>
            <c:when test="${not empty loadError}">
                <p class="shop-error" role="alert">
                    <c:out value="${loadError}" />
                </p>
            </c:when>
            <c:when test="${empty myOrders}">
                <section class="shop-empty">
                    <svg>
                        <use href="#i-calendar" />
                    </svg>
                    <h2>Bạn chưa có đơn thuê nào</h2>
                    <p>Những đơn đã tạo sẽ xuất hiện tại đây để bạn theo dõi.</p>
                    <a class="button" href="${ctx}/home">Khám phá thiết bị →</a>
                </section>
            </c:when>
            <c:otherwise>
                <c:choose>
                    <c:when test="${detailOrder}">
                        <a class="my-orders-back" href="${ctx}/my-rentals">← Tất cả đơn
                            thuê của tôi</a>
                    </c:when>
                    <c:otherwise>
                        <section class="my-order-filters" aria-label="Lọc đơn thuê">
                            <label class="my-order-search">
                                <svg aria-hidden="true">
                                    <use href="#i-search" />
                                </svg>
                                <input type="search" id="my-order-search" placeholder="Tìm mã đơn, tên thiết bị…"
                                    aria-label="Tìm đơn thuê">
                            </label>
                            <label>Trạng thái<select id="my-order-status">
                                    <option value="">Tất cả trạng thái</option>
                                    <c:forEach var="status" items="${['pending','confirmed','renting','returned','cancelled']}">
                                        <option value="${status}">
                                            <c:out value="${rentalLabels[status]}" />
                                        </option>
                                    </c:forEach>
                                </select>
                            </label>
                            <span id="my-order-count" aria-live="polite">${fn:length(myOrders)} đơn
                                thuê</span>
                        </section>
                    </c:otherwise>
                </c:choose>
                <div class="my-orders-list">
                    <c:forEach var="order" items="${myOrders}">
                        <%@include file="/inc/shop/my-order-card.jsp"%>
                    </c:forEach>
                </div>
                <p class="my-order-no-match" id="my-order-no-match" hidden>Không tìm thấy đơn phù hợp. Hãy thử từ khóa
                    hoặc trạng thái khác.</p>
                <p class="my-rental-note">Trạng thái thanh toán được cập nhật sau khi admin kiểm tra tiền thực nhận. Bấm
                    “Cập nhật trạng thái” để xem dữ liệu mới nhất.</p>
            </c:otherwise>
        </c:choose>
    </main>
    <footer class="shop-footer">LensCam Rental Hub · 77 Nguyễn Huệ, Huế · 252 Nguyễn Tất Thành, Đà Nẵng</footer>
</body>

</html>
