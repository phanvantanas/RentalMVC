<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="vi_VN"/><c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Quản lý thiết bị &amp; Kho máy | LensCam Admin</title><link rel="stylesheet" href="${ctx}/assets/css/admin.css?v=1"><link rel="stylesheet" href="${ctx}/assets/css/admin-devices.css?v=2"><script src="${ctx}/assets/js/admin-devices.js?v=2" defer></script></head>
<body><%@include file="/inc/icons.jsp"%><%@include file="/inc/admin/sidebar.jsp"%>
<div class="admin-shell"><header class="admin-topbar"><div>Quản lý Thiết bị &amp; Kho máy <span class="breadcrumb">/ Inventory</span></div><div class="admin-account"><span class="admin-avatar"><svg><use href="#i-camera"/></svg></span><span><c:out value="${sessionScope.user.name}"/><small>QUẢN TRỊ VIÊN</small></span><a href="${ctx}/logout">Đăng xuất ↗</a></div></header>
<main class="admin-main">
    <div class="page-heading"><div><span class="eyebrow">ASSET &amp; INVENTORY MANAGEMENT</span><h1>Quản lý Thiết bị &amp; Kho máy</h1><p>Theo dõi từng thiết bị, số serial và tình trạng cho thuê.</p></div><div class="device-heading-actions"><button class="button secondary" type="button" id="export-devices" aria-label="Xuất danh sách thiết bị">↓ CSV</button><button class="button secondary" type="button" id="add-device">Nhập thêm thiết bị</button><button class="button" type="button" id="add-product"><span>＋</span> Thêm sản phẩm mới</button></div></div>
    <c:if test="${not empty loadError}"><div class="notice error" role="alert"><c:out value="${loadError}"/></div></c:if><c:if test="${not empty flash}"><div class="notice success" role="status"><c:out value="${flash}"/></div></c:if><c:if test="${not empty flashError}"><div class="notice error" role="alert"><c:out value="${flashError}"/></div></c:if>
    <%@include file="/inc/admin/device-stats.jsp"%>
    <%@include file="/inc/admin/device-table.jsp"%>
    <p class="admin-footnote">LENSCAM ASSET CONTROL · Dữ liệu cập nhật từ kho thiết bị</p>
</main></div>
<%@include file="/inc/admin/device-dialogs.jsp"%>
</body></html>
