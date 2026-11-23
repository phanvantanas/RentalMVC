<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quy trình thuê máy | LensCam</title>
    <link rel="stylesheet" href="${ctx}/assets/css/catalog.css?v=20261119-rating-right">
    <link rel="stylesheet" href="${ctx}/assets/css/shop.css?v=20261118-footer-bold">
    <link rel="stylesheet" href="${ctx}/assets/css/customer-workflow.css?v=4-important-notes">
</head>

<body class="customer-process-page">
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/shop/header.jsp"%>
    <main class="process-page">
        <div class="process-intro">
            <h1>PROCESS</h1>
            <p>Hướng dẫn chi tiết từ A-Z quy trình thuê máy ảnh chuyên nghiệp. Đơn giản, minh bạch, an toàn.</p>
        </div>
        <%@include file="/inc/shop/workflow-content.jsp"%>
    </main>
    <footer class="shop-footer">LensCam Rental Hub · 77 Nguyễn Huệ, Huế · 252 Nguyễn Tất Thành, Đà Nẵng</footer>
</body>

</html>
