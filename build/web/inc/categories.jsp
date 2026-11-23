<%@page pageEncoding="UTF-8"%>
<section class="category-strip" aria-label="Danh mục sản phẩm">
    <div class="container category-inner">
        <details class="category-filter" id="category-filter">
            <summary aria-label="Lọc theo hãng" aria-controls="category-tabs" title="Lọc theo hãng">
                <svg aria-hidden="true">
                    <use href="#i-filter" />
                </svg>
            </summary>
            <div class="category-filter-menu">
                <strong id="category-filter-heading">Hãng thiết bị</strong>
                <div class="category-tabs" id="category-tabs" role="group" aria-labelledby="category-filter-heading">
                    <button type="button" class="category-tab active" data-category="">Tất cả
                        <span>${fn:length(lstProduct)}</span>
                    </button>
                    <c:forEach items="${lstCategory}" var="cat">
                        <button type="button" class="category-tab" data-category="${cat.id}">
                            <c:out value="${cat.name}" />
                            <span>${empty categoryCounts[cat.id] ? 0 : categoryCounts[cat.id]}</span>
                        </button>
                    </c:forEach>
                </div>
            </div>
        </details>
        <span class="category-filter-selection" id="category-filter-selection" aria-live="polite">Tất cả
            hãng</span>
    </div>
</section>
