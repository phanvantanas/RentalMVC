<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Tổng quan đơn thuê | LensCam Admin</title><link rel="stylesheet" href="${ctx}/assets/css/admin.css?v=20261118-workflow"><link rel="stylesheet" href="${ctx}/assets/css/admin-overview.css?v=1"><script src="${ctx}/assets/js/admin-overview.js" defer></script></head>
<body><%@include file="/inc/icons.jsp"%><%@include file="/inc/admin/sidebar.jsp"%>
<div class="admin-shell">
<header class="admin-topbar"><div>LensCam Admin Console <span class="breadcrumb">/ Tổng quan</span></div><div class="admin-account"><span class="admin-avatar"><svg><use href="#i-camera"/></svg></span><span><c:out value="${sessionScope.user.name}"/><small>QUẢN TRỊ VIÊN</small></span><a href="${ctx}/logout">Đăng xuất ↗</a></div></header>
<main class="admin-main overview-main">
<div class="page-heading overview-heading"><div><span class="eyebrow"><span class="live-dot"></span> RENTAL OPERATIONS</span><h1>Bảng Điều Khiển Tổng Quan</h1><p>Số liệu đơn thuê và tình hình sử dụng thiết bị.</p></div>
<div class="overview-actions"><form method="get" action="${ctx}/admin" class="period-form">
<div class="period-tabs"><button name="period" value="today" class="${period eq 'today' ? 'selected' : ''}">Hôm nay</button><button name="period" value="week" class="${period eq 'week' ? 'selected' : ''}">7 ngày qua</button><button name="period" value="month" class="${period eq 'month' ? 'selected' : ''}">Tháng này</button><button type="button" id="custom-toggle" aria-expanded="${period eq 'custom'}" class="${period eq 'custom' ? 'selected' : ''}">Tùy chỉnh ▾</button></div>
<div id="custom-range" class="custom-range" ${period ne 'custom' ? 'hidden' : ''}><label>Từ ngày<input type="date" name="from" value="${from}" required></label><label>Đến ngày<input type="date" name="to" value="${to}" required></label><button class="button" name="period" value="custom">Áp dụng</button></div></form>
<div class="overview-tools"><c:url var="exportUrl" value="/admin"><c:param name="period" value="${period}"/><c:param name="from" value="${from}"/><c:param name="to" value="${to}"/><c:param name="export" value="csv"/></c:url><a class="button secondary" href="<c:out value='${exportUrl}'/>">↓ Xuất báo cáo</a><button class="button" id="refresh-dashboard" type="button">↻ Làm mới dữ liệu</button></div></div></div>
<c:choose><c:when test="${not empty loadError}"><div class="notice error" role="alert"><c:out value="${loadError}"/></div></c:when><c:otherwise>
<p class="overview-range">Kỳ báo cáo: ${from} → ${to} <span>· Kho thiết bị cập nhật theo trạng thái hiện tại</span></p>
<%@include file="/inc/admin/overview-stats.jsp"%>
<div class="dashboard-layout"><div class="dashboard-primary"><%@include file="/inc/admin/overview-chart.jsp"%><%@include file="/inc/admin/overview-products.jsp"%></div><%@include file="/inc/admin/overview-activity.jsp"%></div>
</c:otherwise></c:choose>
<p class="admin-footnote">LENSCAM ADMIN CONSOLE · Chỉ dành cho quản trị viên</p>
</main></div></body></html>
