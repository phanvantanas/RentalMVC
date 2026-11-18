<%@page pageEncoding="UTF-8"%>
<section class="category-strip" aria-label="Danh mục sản phẩm"><div class="container category-inner">
        <div class="category-tabs" id="category-tabs"><button type="button" class="category-tab active" data-category="">Tất cả <span>${fn:length(lstProduct)}</span></button><c:forEach items="${lstCategory}" var="cat"><button type="button" class="category-tab" data-category="${cat.id}"><c:out value="${cat.name}"/> <span>${empty categoryCounts[cat.id] ? 0 : categoryCounts[cat.id]}</span></button></c:forEach></div>
    </div></section>
