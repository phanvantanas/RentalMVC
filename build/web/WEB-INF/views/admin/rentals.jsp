<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="theme-color" content="#213145"><title>Quản lý đơn thuê | LensCam Admin</title><link rel="stylesheet" href="${ctx}/assets/css/admin.css?v=1"><script src="${ctx}/assets/js/admin-rentals.js" defer></script></head>
<body>
<%@include file="/inc/icons.jsp"%>
<%@include file="/inc/admin/sidebar.jsp"%>
<div class="admin-shell">
    <header class="admin-topbar"><div><span>Quản lý Đơn thuê &amp; Vận hành Hub</span><span class="breadcrumb">/ Console</span></div><div class="admin-account"><span class="admin-avatar"><svg><use href="#i-camera"/></svg></span><span><c:out value="${sessionScope.user.name}"/><small>QUẢN TRỊ VIÊN</small></span><a href="${ctx}/logout">Đăng xuất ↗</a></div></header>
    <main class="admin-main">
        <div class="page-heading"><div><span class="eyebrow">RENTAL OPERATIONS</span><h1>Quản lý đơn thuê</h1><p>Theo dõi lịch thuê, duyệt đơn và điều phối thiết bị.</p></div><button class="button secondary" type="button" id="refresh-orders"><svg><use href="#i-calendar"/></svg>Tải lại dữ liệu</button></div>
        <c:if test="${not empty loadError}"><div class="notice error" role="alert"><c:out value="${loadError}"/></div></c:if>
        <c:if test="${not empty flash}"><div class="notice success" role="status"><c:out value="${flash}"/></div></c:if>
        <c:if test="${not empty flashError}"><div class="notice error" role="alert"><c:out value="${flashError}"/></div></c:if>
        <%@include file="/inc/admin/stats.jsp"%>
        <div class="operations-layout">
            <div class="operations-main"><%@include file="/inc/admin/orders.jsp"%><%@include file="/inc/admin/workflow.jsp"%></div>
            <%@include file="/inc/admin/details.jsp"%>
        </div>
        <p class="admin-footnote">LENSCAM ADMIN CONSOLE · Chỉ dành cho quản trị viên</p>
    </main>
</div>
</body></html>
