<%@page pageEncoding="UTF-8"%>
<form class="rental-search" id="search-form" action="${ctx}/home" method="get" data-days="${rentalDays}" data-product="<c:out value='${param.product}'/>">
    <div class="rental-offers" aria-label="Ưu đãi theo thời gian thuê">
        <h2><svg><use href="#i-gift"/></svg>Ưu đãi theo thời gian thuê</h2>
        <div class="offer-tiers">
            <span class="offer-tier offer-three" data-min-days="3"><strong>-10%</strong> ≥ 3 ngày</span>
            <span class="offer-tier offer-five" data-min-days="5"><strong>-15%</strong> ≥ 5 ngày</span>
            <span class="offer-tier offer-seven" data-min-days="7"><strong>-20%</strong> ≥ 7 ngày</span>
        </div>
    </div>
    <div class="rental-fields">
        <label class="rental-field" for="category-select"><span><svg><use href="#i-camera"/></svg>Loại thiết bị</span>
            <select id="category-select" name="id_category"><option value="">📷 Tất cả loại thiết bị</option><c:forEach items="${lstCategory}" var="cat"><option value="${cat.id}" ${param.id_category eq cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option></c:forEach></select>
        </label>
        <label class="rental-field" for="device-select"><span><svg><use href="#i-settings"/></svg>Thiết bị cụ thể</span>
            <select id="device-select" name="product" disabled><option value="">Chọn loại thiết bị trước...</option></select>
        </label>
        <label class="rental-field" for="start-date"><span><svg><use href="#i-calendar"/></svg>Ngày nhận *</span><input type="date" id="start-date" name="start" min="${today}" value="${startDate}" required></label>
        <label class="rental-field" for="end-date"><span><svg><use href="#i-calendar"/></svg>Ngày trả *</span><input type="date" id="end-date" name="end" min="${today}" value="${endDate}" required></label>
        <label class="rental-field" for="pickup-time"><span><svg><use href="#i-clock"/></svg>Giờ nhận *</span><select id="pickup-time" name="pickup_time" required><c:forEach begin="8" end="18" var="hour"><c:set var="timeValue" value="${hour lt 10 ? '0' : ''}${hour}:00"/><option value="${timeValue}" ${pickupTime eq timeValue ? 'selected' : ''}>${timeValue} - ${hour lt 12 ? 'Sáng' : (hour lt 18 ? 'Chiều' : 'Tối')}</option></c:forEach></select></label>
        <label class="rental-field" for="return-time"><span><svg><use href="#i-clock"/></svg>Giờ trả *</span><select id="return-time" name="return_time" required><c:forEach begin="8" end="18" var="hour"><c:set var="timeValue" value="${hour lt 10 ? '0' : ''}${hour}:00"/><option value="${timeValue}" ${returnTime eq timeValue ? 'selected' : ''}>${timeValue} - ${hour lt 12 ? 'Sáng' : (hour lt 18 ? 'Chiều' : 'Tối')}</option></c:forEach></select></label>
    </div>
    <div class="rental-search-options"><label><input type="checkbox" id="available-only" name="available" value="1" ${param.available eq '0' ? '' : 'checked'}>Chỉ hiển thị thiết bị có sẵn</label><span id="rental-days" aria-live="polite">${rentalDays} ngày</span></div>
    <c:if test="${not empty dateError}"><p class="form-error" role="alert"><c:out value="${dateError}"/></p></c:if>
    <div class="rental-search-action"><button class="button" type="submit"><svg><use href="#i-search"/></svg>TÌM KIẾM THIẾT BỊ</button></div>
</form>
