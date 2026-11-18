<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<%@include file="/inc/head.jspf"%>
<body>
<%@include file="/inc/icons.jspf"%>
<%@include file="/inc/header.jspf"%>
<main>
    <%@include file="/inc/hero.jspf"%>
    <%@include file="/inc/categories.jspf"%>
    <%@include file="/inc/catalog-controls.jspf"%>
    <div class="container catalog-layout" id="catalog">
        <%@include file="/inc/catalog.jspf"%>
    </div>
</main>
<%@include file="/inc/footer.jspf"%>
<%@include file="/inc/rental-dialog.jspf"%>
<%@include file="/inc/guide-dialog.jspf"%>
<%@include file="/inc/history-dialog.jspf"%>
</body>
</html>
