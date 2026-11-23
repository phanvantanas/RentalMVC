<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quản lý người dùng | LensCam Admin</title>
    <link rel="stylesheet" href="${ctx}/assets/css/admin.css?v=20261119-type600">
    <link rel="stylesheet" href="${ctx}/assets/css/admin-users.css?v=20261119-type600">
    <script src="${ctx}/assets/js/admin-users.js?v=3-no-admin-lock" defer></script>
</head>

<body>
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/admin/sidebar.jsp"%>
    <div class="admin-shell">
        <header class="admin-topbar">
            <div>LensCam Admin Console <span class="breadcrumb">/ Quản lý người dùng</span>
            </div>
            <div class="admin-account">
                <span class="admin-avatar">
                    <svg>
                        <use href="#i-users" />
                    </svg>
                </span>
                <span>
                    <c:out value="${sessionScope.user.name}" />
                    <small>QUẢN TRỊ VIÊN</small>
                </span>
                <a href="${ctx}/logout">Đăng xuất ↗</a>
            </div>
        </header>
        <main class="admin-main users-main">
            <div class="page-heading">
                <div>
                    <span class="eyebrow">HỆ THỐNG / QUẢN LÝ NGƯỜI DÙNG</span>
                    <h1>Quản lý Người dùng</h1>
                    <p>Quản lý tài khoản, quyền truy cập và lịch sử thuê thiết bị.</p>
                </div>
                <div class="user-heading-actions">
                    <button class="button secondary" type="button" id="export-users" ${not empty loadError ? 'disabled' : ''}>
                        <svg aria-hidden="true">
                            <use href="#i-download" />
                        </svg>Xuất CSV
                    </button>
                    <button class="button" type="button" id="add-user" ${not empty loadError ? 'disabled' : ''}>
                        <svg aria-hidden="true">
                            <use href="#i-users" />
                        </svg>Thêm người dùng mới
                    </button>
                </div>
            </div>
            <c:if test="${not empty userFlash}">
                <p class="notice success" role="status">
                    <c:out value="${userFlash}" />
                </p>
            </c:if>
            <c:if test="${not empty userError}">
                <p class="notice error" role="alert">
                    <c:out value="${userError}" />
                </p>
            </c:if>
            <c:choose>
                <c:when test="${not empty loadError}">
                    <p class="notice error" role="alert">
                        <c:out value="${loadError}" />
                    </p>
                </c:when>
                <c:otherwise>
                    <section class="user-stat-grid" aria-label="Tổng quan người dùng">
                        <div class="user-stat">
                            <span>TỔNG NGƯỜI DÙNG</span>
                            <span class="user-stat-icon">
                                <svg>
                                    <use href="#i-users" />
                                </svg>
                            </span>
                            <strong>
                                <fmt:formatNumber value="${userStats.total}" />
                            </strong>
                            <small>Tất cả tài khoản trong hệ thống</small>
                        </div>
                        <div class="user-stat customer">
                            <span>KHÁCH HÀNG</span>
                            <span class="user-stat-icon">
                                <svg>
                                    <use href="#i-check" />
                                </svg>
                            </span>
                            <strong>
                                <fmt:formatNumber value="${empty userStats.user ? 0 : userStats.user}" />
                            </strong>
                            <small>Tài khoản thuê thiết bị</small>
                        </div>
                        <div class="user-stat admin">
                            <span>QUẢN TRỊ VIÊN</span>
                            <span class="user-stat-icon">
                                <svg>
                                    <use href="#i-settings" />
                                </svg>
                            </span>
                            <strong>
                                <fmt:formatNumber value="${empty userStats.admin ? 0 : userStats.admin}" />
                            </strong>
                            <small>Được truy cập khu vực quản trị</small>
                        </div>
                        <div class="user-stat locked">
                            <span>TÀI KHOẢN BỊ KHÓA</span>
                            <span class="user-stat-icon">
                                <svg>
                                    <use href="#i-lock" />
                                </svg>
                            </span>
                            <strong>
                                <fmt:formatNumber value="${empty userStats.locked ? 0 : userStats.locked}" />
                            </strong>
                            <small>Đã ngừng quyền đăng nhập</small>
                        </div>
                    </section>
                    <%@include file="/inc/admin/user-table.jsp"%>
                    <%@include file="/inc/admin/user-dialogs.jsp"%>
                </c:otherwise>
            </c:choose>
            <p class="admin-footnote">LENSCAM USER MANAGEMENT · Chỉ dành cho quản trị viên</p>
        </main>
    </div>
</body>

</html>
