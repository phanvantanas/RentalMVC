<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f8f9ff">
    <title>Đăng nhập LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/auth.css?v=1">
    <script src="${ctx}/assets/js/auth.js?v=20261118-login" defer></script>
</head>
<body class="auth-page">
<%@include file="/inc/icons.jsp"%>
<main class="auth-shell">
    <div class="auth-node"><span></span>CINE RENTAL AUTH NODE</div>
    <%@include file="/inc/login-form.jsp"%>
    <p class="auth-footnote">Đăng nhập để quản lý lịch thuê và thiết bị của bạn tại LensCam.</p>
    <button class="auth-support" type="button" data-auth-dialog="help-dialog" aria-label="Hỗ trợ đăng nhập"><svg><use href="#i-headset"/></svg></button>
</main>
<dialog id="help-dialog" class="auth-dialog" aria-labelledby="help-title">
    <form method="dialog"><button class="dialog-close" aria-label="Đóng">×</button></form>
    <span class="auth-node">LENSCAM SUPPORT</span>
    <h2 id="help-title">Hỗ trợ tài khoản</h2>
    <p>Nếu quên mật khẩu, vui lòng liên hệ trực tiếp tại điểm nhận máy để được hỗ trợ xác minh và khôi phục tài khoản.</p>
    <div class="support-location"><strong>Hub Huế</strong><span>77 Nguyễn Huệ, Huế</span></div>
    <div class="support-location"><strong>Hub Đà Nẵng</strong><span>252 Nguyễn Tất Thành, Đà Nẵng</span></div>
    <a class="auth-home-link" href="${ctx}/home">Về trang chủ <svg><use href="#i-arrow"/></svg></a>
</dialog>
</body>
</html>
