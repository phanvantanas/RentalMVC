<%@page pageEncoding="UTF-8"%>
<section class="device-filter-panel" aria-label="Lọc kho thiết bị">
    <div class="device-filter-row">
        <label class="device-search">
            <svg>
                <use href="#i-search" />
            </svg>
            <input id="device-search" type="search" placeholder="Tên máy, serial, mã thiết bị…"
                aria-label="Tìm thiết bị">
        </label>
        <label class="inventory-select">
            <span>DANH MỤC:</span>
            <select id="device-category">
                <option value="">Tất cả danh mục</option>
                <c:forEach items="${categories}" var="cat">
                    <option value="${cat.id}">
                        <c:out value="${cat.name}" />
                    </option>
                </c:forEach>
            </select>
        </label>
        <label class="inventory-select">
            <span>HÃNG:</span>
            <select id="device-brand">
                <option value="">Tất cả hãng</option>
            </select>
        </label>
        <label class="inventory-select">
            <span>TRẠNG THÁI:</span>
            <select id="device-status">
                <option value="">Tất cả trạng thái</option>
                <c:forEach items="${['available','renting','maintenance','reserved','inactive']}" var="status">
                    <option value="${status}">
                        <c:out value="${deviceStatusLabels[status]}" />
                    </option>
                </c:forEach>
            </select>
        </label>
        <button class="icon-button" id="device-reset" type="button" aria-label="Xóa bộ lọc">
            <svg>
                <use href="#i-filter" />
            </svg>
        </button>
    </div>
    <div class="device-bulk-bar">
        <span>
            <strong id="selection-count" aria-live="polite">Đã chọn: 0 thiết
                bị</strong>
            <small>Thao tác áp dụng cho danh sách đã chọn</small>
        </span>
        <div>
            <button type="button" data-bulk="price" disabled>↗ Cập nhật giá thuê</button>
            <button type="button" data-bulk="status" disabled>⇄ Chuyển trạng thái</button>
            <button type="button" data-bulk="delete" class="bulk-delete" disabled>♜ Xóa đã chọn</button>
        </div>
    </div>
</section>
<section class="orders-panel device-table-panel" aria-label="Danh sách thiết bị">
    <div class="device-table-scroll">
        <table class="device-table">
            <thead>
                <tr>
                    <th class="check-cell">
                        <input type="checkbox" id="select-device-page" aria-label="Chọn thiết bị trên trang hiện tại">
                    </th>
                    <th>THIẾT BỊ &amp; ASSET TAG</th>
                    <th>SỐ SERIAL (SN)</th>
                    <th>DANH MỤC &amp; HÃNG</th>
                    <th>GIÁ THUÊ / TIỀN CỌC</th>
                    <th>TÌNH TRẠNG / LỊCH THUÊ</th>
                    <th>TRẠNG THÁI</th>
                    <th>THAO TÁC</th>
                </tr>
            </thead>
            <tbody id="device-rows">
                <c:forEach items="${devices}" var="d">
                    <tr class="device-row" data-id="${d.id}" data-product="${d.productId}"
                        data-name="<c:out value='${d.productName}'/>" data-serial="<c:out value='${d.serial}'/>"
                        data-category="${d.categoryId}" data-status="${d.effectiveStatus}" data-physical="${d.status}"
                        data-price="${d.price}">
                        <td class="check-cell">
                            <input type="checkbox" class="device-checkbox" aria-label="Chọn thiết bị DEV-${d.id}">
                        </td>
                        <td>
                            <div class="device-asset">
                                <c:choose>
                                    <c:when test="${not empty deviceImages[d.id]}">
                                        <img src="<c:out value='${deviceImages[d.id]}'/>" alt="<c:out value='${d.productName}'/>"
                                            loading="lazy">
                                    </c:when>
                                    <c:otherwise>
                                        <span class="device-image-placeholder">
                                            <svg>
                                                <use href="#i-camera" />
                                            </svg>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                                <div>
                                    <strong>
                                        <c:out value="${d.productName}" />
                                    </strong>
                                    <small>Asset: DEV-
                                        <fmt:formatNumber value="${d.id}" pattern="0000" />
                                    </small>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="serial-tag">
                                <c:out value="${d.serial}" />
                            </span>
                        </td>
                        <td>
                            <span>
                                <c:out value="${d.categoryName}" />
                            </span>
                            <small class="device-brand-label"></small>
                        </td>
                        <td>
                            <strong class="inventory-money">
                                <fmt:formatNumber value="${d.price}" maxFractionDigits="0" />đ
                            </strong>
                            <small>/ngày</small>
                            <button type="button" class="device-price-edit" data-edit-price="${d.id}"
                                aria-label="Sửa giá thuê DEV-${d.id}">
                                <svg aria-hidden="true">
                                    <use href="#i-edit" />
                                </svg>Sửa giá
                            </button>
                            <span class="device-deposit">Tiền cọc: Liên hệ</span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${d.rentalId gt 0}">
                                    <a class="device-rental-link"
                                        href="${ctx}/admin/rentals?order=${d.rentalId}">Đơn #ORD-${d.rentalId}
                                        ↗</a>
                                </c:when>
                                <c:when test="${d.effectiveStatus eq 'reserved'}">
                                    <span>Đã giữ lịch thuê hôm nay</span>
                                </c:when>
                                <c:when test="${d.status eq 'maintenance'}">
                                    <span class="danger-text">Đang kiểm tra / bảo trì</span>
                                </c:when>
                                <c:otherwise>
                                    <span>Trong kho</span>
                                </c:otherwise>
                            </c:choose>
                            <small class="mono-muted">${d.productStatus eq 'active' ? 'Mẫu máy đang hoạt động' : 'Mẫu máy ngừng cho thuê'}</small>
                        </td>
                        <td>
                            <span class="device-status-badge ${d.effectiveStatus}">
                                <c:out value="${deviceStatusLabels[d.effectiveStatus]}" />
                            </span>
                        </td>
                        <td>
                            <div class="device-row-actions">
                                <button type="button" data-edit="${d.id}"
                                    aria-label="Sửa thiết bị DEV-${d.id}" title="Sửa thiết bị">✎</button>
                                <button type="button" data-delete="${d.id}" class="delete-device"
                                    aria-label="Xóa thiết bị DEV-${d.id}" title="Xóa thiết bị">×</button>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <div class="orders-empty" id="devices-empty" hidden>
            <svg>
                <use href="#i-camera" />
            </svg>
            <strong>Chưa tìm thấy thiết bị phù hợp</strong>
            <p>Thử thay đổi từ khóa hoặc điều kiện lọc.</p>
        </div>
    </div>
    <div class="table-pagination">
        <span id="device-page-label" aria-live="polite"></span>
        <div>
            <button type="button" id="device-previous">← Trước</button>
            <span id="device-page-number">1 /
                1</span>
            <button type="button" id="device-next">Sau →</button>
        </div>
    </div>
</section>
