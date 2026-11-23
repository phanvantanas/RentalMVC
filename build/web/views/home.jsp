<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<%@include file="/inc/head.jsp"%>

<body>
    <%@include file="/inc/icons.jsp"%>
    <%@include file="/inc/header.jsp"%>
    <main>
        <%@include file="/inc/hero.jsp"%>
        <%@include file="/inc/categories.jsp"%>
        <%@include file="/inc/catalog-controls.jsp"%>
        <div class="container catalog-layout" id="catalog">
            <%@include file="/inc/catalog.jsp"%>
        </div>
    </main>
    <%@include file="/inc/footer.jsp"%>
    <%@include file="/inc/rental-dialog.jsp"%>
    <%@include file="/inc/guide-dialog.jsp"%>
    <%@include file="/inc/history-dialog.jsp"%>
</body>

</html>
