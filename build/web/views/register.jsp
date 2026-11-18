<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f8f9ff">
    <title>Tạo tài khoản LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/auth.css?v=20261118-weight500">
    <link rel="stylesheet" href="${ctx}/assets/css/register.css?v=1">
    <script src="${ctx}/assets/js/register.js" defer></script>
</head>
<body class="register-page">
<%@include file="/inc/icons.jsp"%>
<header class="register-header"><a class="auth-brand" href="${ctx}/home" aria-label="LensCam trang chủ"><span class="auth-brand-icon"><svg><use href="#i-camera"/></svg></span><span><span class="auth-brand-name">LENSCAM <small>CINE GEAR</small></span><span class="auth-brand-caption">PRO RENTAL INFRASTRUCTURE</span></span></a><a class="register-header-login" href="${ctx}/login">Đã có tài khoản? <strong>Đăng nhập ↗</strong></a></header>
<main class="register-shell">
    <div class="auth-node"><span></span>HỆ THỐNG ĐỊNH DANH NGƯỜI THUÊ CINE V2.4</div>
    <%@include file="/inc/register-form.jsp"%>
</main>
</body>
</html>
