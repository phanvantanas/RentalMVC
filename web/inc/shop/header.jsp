<%@page pageEncoding="UTF-8"%>
<header class="shop-header">
    <a class="brand" href="${pageContext.request.contextPath}/home">
        <span class="brand-icon">
            <svg>
                <use href="#i-camera" />
            </svg>
        </span>
        <span>LensCam<small>RENTAL HUB</small>
        </span>
    </a>
    <nav>
        <a href="${pageContext.request.contextPath}/home">Khám phá thiết bị</a>
        <a href="${pageContext.request.contextPath}/my-rentals">Đơn thuê của tôi</a>
        <a href="${pageContext.request.contextPath}/workflow" ${pageContext.request.servletPath eq '/WEB-INF/views/shop/workflow.jsp' ? 'aria-current="page"' : ''}>Quy trình</a>
        <a class="header-cart" href="${pageContext.request.contextPath}/cart">
            <svg class="cart-menu-icon" aria-hidden="true">
                <use href="#i-cart" />
            </svg>Giỏ hàng <span>${empty sessionScope.rentalCart ? 0 : sessionScope.rentalCart.count}</span>
        </a>
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <a class="shop-user" href="${pageContext.request.contextPath}/profile" title="Chỉnh sửa thông tin cá nhân">Hồ sơ ·
                    <c:out value="${sessionScope.user.name}" />
                </a>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
            </c:otherwise>
        </c:choose>
    </nav>
</header>

<%@include file="/inc/shop/chatbot.jsp"%>
