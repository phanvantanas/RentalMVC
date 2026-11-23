<%@page pageEncoding="UTF-8"%>
<article class="product-card ${stock eq 0 ? 'unavailable' : ''}" data-id="${prod.id}" data-name="<c:out value='${prod.name}'/>"
    data-category="${prod.id_category}" data-price="${prod.price}" data-deposit="${prod.deposit}" data-deposit-known="${prod.depositConfigured}"
    data-stock="${stock}" data-order="${loop.index}">
    <div class="product-visual">
        <div class="product-badges">
            <span class="stock-badge ${stock eq 0 ? 'out-of-stock' : ''}">
                <span>●</span> ${stock gt 0 ? 'Có sẵn' : 'Chưa có máy trống'}<c:if test="${stock gt 0}"> (${stock} máy)</c:if>
            </span>
        </div>
        <c:choose>
            <c:when test="${not empty productImages[prod.id]}">
                <img src="<c:out value='${productImages[prod.id]}'/>" alt="<c:out value='${prod.name}'/>" loading="${loop.index lt 3 ? 'eager' : 'lazy'}"
                    decoding="async">
            </c:when>
            <c:otherwise>
                <div class="image-placeholder">
                    <svg>
                        <use href="#i-camera" />
                    </svg>
                    <span>Đang cập nhật ảnh</span>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="product-info">
        <div class="product-eyebrow">
            <c:out value="${categoryNames[prod.id_category]}" />
        </div>
        <h3>
            <c:out value="${prod.name}" />
        </h3>
        <p class="product-description">
            <c:out value="${empty prod.description ? 'Thiết bị dành cho nhiếp ảnh và sáng tạo nội dung.' : prod.description}" />
        </p>
        <c:if test="${prod.depositConfigured}">
            <div class="deposit-tag">Cọc:
                <fmt:formatNumber value="${prod.deposit}" maxFractionDigits="0" />đ
            </div>
        </c:if>
        <div class="product-footer">
            <div class="product-price-row">
                <div class="daily-price">
                    <strong>
                        <fmt:formatNumber value="${prod.price}" maxFractionDigits="0" />đ
                    </strong>
                    <span>/ngày</span>
                </div>
                <div class="product-rating" aria-label="Đánh giá 4,9 trên 5">
                    <span class="rating-star" aria-hidden="true">★</span>
                    <strong>4.9</strong>
                    <span>/5</span>
                </div>
            </div>
            <button type="button" class="button rent-button" ${stock eq 0 ? 'disabled' : ''}>${stock gt 0 ? 'Thuê ngay' : 'Chưa có máy trống'}<c:if test="${stock gt 0}">
                    <svg>
                        <use href="#i-arrow" />
                    </svg>
                </c:if>
            </button>
        </div>
    </div>
</article>
