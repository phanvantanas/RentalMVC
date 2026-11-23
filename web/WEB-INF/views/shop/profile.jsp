<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Thông tin cá nhân | LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/catalog.css?v=20261119-rating-right">
    <link rel="stylesheet" href="${ctx}/assets/css/shop.css?v=20261118-footer-bold">
    <link rel="stylesheet" href="${ctx}/assets/css/profile.css?v=3-password">
</head>

<body>
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/shop/header.jsp"%>
    <main class="shop-container profile-page">
        <div class="shop-page-heading">
            <span class="eyebrow">TÀI KHOẢN LENSCAM</span>
            <h1>Thông tin cá nhân</h1>
            <p>Cập nhật thông tin liên hệ để LensCam hỗ trợ đơn thuê của bạn.</p>
        </div>
        <section class="profile-card" aria-labelledby="profile-title">
            <h2 id="profile-title">Chỉnh sửa hồ sơ</h2>
            <c:if test="${not empty profileSuccess}">
                <p class="profile-success" role="status">
                    <c:out value="${profileSuccess}" />
                </p>
            </c:if>
            <c:if test="${not empty profileError}">
                <p class="shop-error" role="alert">
                    <c:out value="${profileError}" />
                </p>
            </c:if>
            <form action="${ctx}/profile" method="post" class="profile-form">
                <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
                <label for="profile-name">Họ và tên <span>*</span>
                </label>
                <input id="profile-name" name="name" value="<c:out value='${profileForm.name}'/>" maxlength="100"
                    autocomplete="name" required>
                <label for="profile-email">Email <span>*</span>
                </label>
                <input id="profile-email" name="email" type="email" value="<c:out value='${profileForm.email}'/>"
                    maxlength="150" autocomplete="email" required>
                <label for="profile-phone">Số điện thoại <span>*</span>
                </label>
                <input id="profile-phone" name="phone" type="tel" value="<c:out value='${profileForm.phone}'/>" maxlength="20"
                    autocomplete="tel" required aria-describedby="phone-help">
                <small id="phone-help">Số điện thoại gồm 9–11 chữ số.</small>
                <div class="profile-actions">
                    <button class="button" type="submit">Lưu thay đổi</button>
                    <a href="${ctx}/home">Về trang chủ</a>
                    <a class="profile-logout" href="${ctx}/logout">Đăng xuất</a>
                </div>
            </form>
        </section>
        <section class="profile-card" aria-labelledby="password-title">
            <h2 id="password-title">Đổi mật khẩu</h2>
            <c:if test="${not empty passwordSuccess}">
                <p class="profile-success" role="status">
                    <c:out value="${passwordSuccess}" />
                </p>
            </c:if>
            <c:if test="${not empty passwordError}">
                <p class="shop-error" role="alert">
                    <c:out value="${passwordError}" />
                </p>
            </c:if>
            <form action="${ctx}/profile" method="post" class="profile-form">
                <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
                <input type="hidden" name="action" value="change_password">
                <label for="current-password">Mật khẩu hiện tại <span>*</span>
                </label>
                <input id="current-password" name="current_password" type="password" maxlength="128"
                    autocomplete="current-password" required>
                <label for="new-password">Mật khẩu mới <span>*</span>
                </label>
                <input id="new-password" name="new_password" type="password" minlength="8" maxlength="128"
                    autocomplete="new-password" aria-describedby="password-help" required>
                <small id="password-help">Từ 8 đến 128 ký tự, khác mật
                    khẩu hiện tại.</small>
                <label for="confirm-password">Nhập lại mật khẩu mới <span>*</span>
                </label>
                <input id="confirm-password" name="confirm_password" type="password" minlength="8" maxlength="128"
                    autocomplete="new-password" required>
                <div class="profile-actions">
                    <button class="button" type="submit">Đổi mật khẩu</button>
                </div>
            </form>
        </section>
    </main>
    <footer class="shop-footer">LensCam Rental Hub · Huế &amp; Đà Nẵng</footer>
</body>

</html>
