<%@page pageEncoding="UTF-8"%>
<div class="announcement"> THIẾT BỊ CHUYÊN NGHIỆP · SẴN SÀNG CHO MỌI KHUNG HÌNH</div>
<header class="site-header">
    <a class="brand" href="${ctx}/home" aria-label="LensCam trang chủ"><span class="brand-icon"><svg><use href="#i-camera"/></svg></span><span>LensCam<small>RENTAL HUB</small></span></a>
    <nav class="main-nav" aria-label="Điều hướng chính">
        <a class="current" href="#catalog">Khám phá thiết bị</a>
        <a href="${ctx}/my-rentals">Đơn thuê của tôi</a>
        <button type="button" data-open="guide-dialog">Hướng dẫn &amp; Giá cọc</button>
        <c:if test="${sessionScope.user.role eq 'admin'}"><a href="${ctx}/admin/rentals">Kênh Admin</a></c:if>
    </nav>
    <div class="header-actions">
        <a class="header-cart" href="${ctx}/cart"><svg class="cart-menu-icon" aria-hidden="true"><use href="#i-cart"/></svg>Giỏ hàng <span>${empty cartCount ? 0 : cartCount}</span></a>
        <a class="date-shortcut" href="#search-form"><svg><use href="#i-calendar"/></svg><span>Chọn ngày nhận &amp; trả</span><kbd>⌘ K</kbd></a>
        <c:choose><c:when test="${not empty sessionScope.user}"><span class="greeting">Chào, <c:out value="${sessionScope.username}"/></span><a href="${ctx}/logout">Đăng xuất</a></c:when><c:otherwise><a href="${ctx}/login">Đăng nhập</a><a class="button" href="${ctx}/register">Đăng ký</a></c:otherwise></c:choose>
    </div>
</header>
