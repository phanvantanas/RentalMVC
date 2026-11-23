<%@page pageEncoding="UTF-8"%>
<section class="orders-panel" aria-label="Danh sách đơn thuê">
    <div class="orders-filters">
        <label class="order-search">
            <svg>
                <use href="#i-search" />
            </svg>
            <input type="search" id="order-search" placeholder="Tìm mã đơn, khách hàng, số điện thoại, thiết bị…"
                aria-label="Tìm đơn thuê">
        </label>
        <div class="order-date-range">
            <label>
                <span>Từ ngày</span>
                <input id="filter-from" type="date" aria-label="Lọc từ ngày">
            </label>
            <span>–</span>
            <label>
                <span>Đến ngày</span>
                <input id="filter-to" type="date" aria-label="Lọc đến ngày">
            </label>
        </div>
        <button type="button" id="clear-search" class="icon-button" aria-label="Xóa bộ lọc">
            <svg>
                <use href="#i-filter" />
            </svg>
        </button>
    </div>
    <p class="payment-filter-note" id="payment-filter-note" hidden>Đang xem đơn chờ đối soát chuyển khoản <button
            type="button" id="clear-payment-filter">Xóa lọc thanh toán</button>
    </p>
    <div class="order-tabs" role="group" aria-label="Lọc trạng thái">
        <button class="active" type="button" data-status="" aria-pressed="true">Tất cả (${fn:length(rentals)})</button>
        <c:forEach items="${['pending','confirmed','renting','returned','cancelled']}" var="s">
            <button type="button" data-status="${s}" aria-pressed="false">
                <span class="mini-status ${s}">
                    <c:out value="${statusLabels[s]}" /> (${empty counts[s] ? 0 : counts[s]})
                </span>
            </button>
        </c:forEach>
    </div>
    <div class="order-table-scroll">
        <table class="order-table">
            <thead>
                <tr>
                    <th>MÃ &amp; KHÁCH HÀNG</th>
                    <th>THIẾT BỊ</th>
                    <th>THỜI GIAN</th>
                    <th>CHI PHÍ</th>
                    <th>TRẠNG THÁI ĐƠN</th>
                    <th>THAO TÁC</th>
                </tr>
            </thead>
            <tbody id="order-rows">
                <c:forEach items="${rentals}" var="r">
                    <tr class="order-row ${r.overdue ? 'is-overdue' : ''}" data-id="${r.id}"
                        data-payment="<c:out value='${r.paymentStatus}'/>" data-status="<c:out value='${r.status}'/>"
                        data-from="${r.startDate}" data-to="${r.endDate}" data-overdue="${r.overdue}">
                        <td>
                            <button type="button" class="order-code" data-detail="${r.id}">#ORD-${r.id}
                            </button>
                            <strong class="customer-name">
                                <c:out value="${empty r.customerName ? 'Khách hàng' : r.customerName}" />
                            </strong>
                            <span class="mono-muted">
                                <c:out value="${r.phone}" />
                            </span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${empty r.items}">
                                    <span class="muted">Chưa có thiết bị</span>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${r.items}" var="item">
                                        <div class="order-device">
                                            <span>
                                                <c:out value="${item.name}" />
                                            </span>
                                            <small>
                                                <c:out value="${item.serial}" />
                                            </small>
                                        </div>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <span>
                                <fmt:formatDate value="${r.startDate}" pattern="dd/MM/yyyy" />
                            </span>
                            <span class="date-end ${r.overdue ? 'danger-text' : ''}">
                                <fmt:formatDate value="${r.endDate}" pattern="dd/MM/yyyy" />
                            </span>
                            <small class="mono-muted">${r.days} ngày</small>
                            <c:if test="${r.overdue}">
                                <span class="overdue-label">Quá hạn trả</span>
                            </c:if>
                        </td>
                        <td>
                            <strong class="money">
                                <fmt:formatNumber value="${r.total}" maxFractionDigits="0" />đ
                            </strong>
                            <span class="cost-caption">Tiền thuê</span>
                            <c:if test="${not empty r.paymentStatus}">
                                <small class="payment-label ${r.paymentStatus}">${r.paymentStatus eq 'paid' ? 'Đã nhận tiền' : (r.paymentStatus eq 'reported' ? 'Chờ đối soát' : 'Chờ chuyển khoản')}</small>
                            </c:if>
                            <c:if test="${r.lateFee gt 0}">
                                <small class="danger-text">Phí trễ:
                                    <fmt:formatNumber value="${r.lateFee}" maxFractionDigits="0" />đ
                                </small>
                            </c:if>
                        </td>
                        <td>
                            <span class="status-badge ${r.status}">
                                <c:out value="${statusLabels[r.status]}" />
                            </span>
                        </td>
                        <td>
                            <button type="button" class="row-detail" data-detail="${r.id}">Chi tiết <svg>
                                    <use href="#i-arrow" />
                                </svg>
                            </button>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <div class="orders-empty" id="orders-empty" hidden>
            <svg>
                <use href="#i-list" />
            </svg>
            <strong>Chưa có đơn thuê phù hợp</strong>
            <p>Thử thay đổi từ khóa hoặc bộ lọc trạng thái.</p>
        </div>
    </div>
    <div class="table-pagination">
        <span id="page-label" aria-live="polite"></span>
        <div>
            <button type="button" id="previous-page">← Trước</button>
            <span id="page-number">1 / 1</span>
            <button type="button" id="next-page">Sau →</button>
        </div>
    </div>
</section>
<c:forEach items="${rentals}" var="r">
    <template id="order-detail-${r.id}">
        <div class="detail-order-title">
            <strong>#ORD-${r.id}</strong>
            <span class="status-badge ${r.status}">
                <c:out value="${statusLabels[r.status]}" />
            </span>
        </div>
        <dl class="detail-fields">
            <div>
                <dt>Khách hàng</dt>
                <dd>
                    <c:out value="${r.customerName}" />
                </dd>
            </div>
            <div>
                <dt>Số điện thoại</dt>
                <dd>
                    <c:out value="${r.phone}" />
                </dd>
            </div>
            <div>
                <dt>Email</dt>
                <dd>
                    <c:out value="${r.email}" />
                </dd>
            </div>
            <div>
                <dt>Ngày nhận</dt>
                <dd>
                    <fmt:formatDate value="${r.startDate}" pattern="dd/MM/yyyy" />
                    <c:out value="${r.pickupTime}" />
                </dd>
            </div>
            <div>
                <dt>Ngày trả</dt>
                <dd>
                    <fmt:formatDate value="${r.endDate}" pattern="dd/MM/yyyy" />
                    <c:out value="${r.returnTime}" />
                </dd>
            </div>
            <div>
                <dt>Tổng tiền thuê</dt>
                <dd class="money">
                    <fmt:formatNumber value="${r.total}" maxFractionDigits="0" />đ
                </dd>
            </div>
            <div>
                <dt>Phí trả trễ</dt>
                <dd>
                    <fmt:formatNumber value="${r.lateFee}" maxFractionDigits="0" />đ
                </dd>
            </div>
        </dl>
        <h3 class="detail-subtitle">Thiết bị trong đơn (${fn:length(r.items)})</h3>
        <c:forEach items="${r.items}" var="item">
            <div class="detail-device">
                <svg>
                    <use href="#i-camera" />
                </svg>
                <span>
                    <strong>
                        <c:out value="${item.name}" />
                    </strong>
                    <small>
                        <c:out value="${item.serial}" />
                    </small>
                </span>
            </div>
        </c:forEach>
        <%@include file="/inc/admin/rental-return-info.jsp"%>
        <c:if test="${r.status eq 'pending' or r.status eq 'confirmed' or r.status eq 'renting'}">
            <form class="status-action-form" action="${ctx}/admin/rentals" method="post">
                <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
                <input type="hidden" name="id" value="${r.id}">
                <input type="hidden" name="expected" value="<c:out value='${r.status}'/>">
                <c:choose>
                    <c:when test="${r.status eq 'pending'}">
                        <button class="button" type="submit" name="status" value="confirmed">Duyệt đơn thuê <svg>
                                <use href="#i-check" />
                            </svg>
                        </button>
                    </c:when>
                    <c:when test="${r.status eq 'confirmed'}">
                        <button class="button" type="submit" name="status" value="renting">Xác
                            nhận bàn giao <svg>
                                <use href="#i-camera" />
                            </svg>
                        </button>
                    </c:when>
                    <c:otherwise>
                        <button class="button green" type="button" data-open-return="${r.id}"
                            data-return-date="${r.endDate}" data-return-time="<c:out value='${r.returnTime}'/>"
                            data-return-start="${r.startDate}" data-daily-rate="${r.dailyRate}">Nhận lại máy &amp; tính
                            phí <svg>
                                <use href="#i-check" />
                            </svg>
                        </button>
                    </c:otherwise>
                </c:choose>
                <c:if test="${r.status ne 'renting' and r.paymentStatus ne 'paid'}">
                    <button class="button cancel-button" type="submit" name="status" value="cancelled">Hủy đơn thuê
                    </button>
                </c:if>
            </form>
        </c:if>
        <c:choose>
            <c:when test="${not empty r.paymentStatus}">
                <div class="payment-admin-detail">
                    <h3 class="detail-subtitle">Thanh toán VietQR</h3>
                    <p>Mã chuyển khoản: <strong>
                            <c:out value="${r.transferReference}" />
                        </strong>
                    </p>
                    <p>${r.paymentStatus eq 'paid' ? 'Đã xác nhận nhận tiền' : (r.paymentStatus eq 'reported' ? 'Khách báo đã chuyển khoản, cần đối soát' : 'Chờ khách chuyển khoản')}</p>
                    <c:if test="${r.paymentStatus ne 'paid' and r.status ne 'cancelled'}">
                        <form class="status-action-form" action="${ctx}/admin/rentals" method="post">
                            <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
                            <input type="hidden" name="id" value="${r.id}">
                            <input type="hidden" name="action" value="confirm_payment">
                            <button class="button green" type="submit">Xác nhận đã nhận tiền</button>
                        </form>
                    </c:if>
                    <c:if test="${r.receiptAvailable}">
                        <div class="admin-receipt">
                            <strong>Ảnh chuyển khoản khách gửi</strong>
                            <a href="${ctx}/payment/receipt?order=${r.id}" target="_blank" rel="noopener">
                                <img src="${ctx}/payment/receipt?order=${r.id}"
                                    alt="Biên lai đơn ORD-${r.id}" loading="lazy">Xem ảnh đầy đủ ↗
                            </a>
                        </div>
                    </c:if>
                    <small>Chỉ xác nhận sau khi kiểm tra tiền thực nhận trong tài khoản ngân hàng.</small>
                </div>
            </c:when>
            <c:otherwise>
                <p class="detail-note">Đơn cũ chưa có dữ liệu thanh toán.</p>
            </c:otherwise>
        </c:choose>
    </template>
</c:forEach>
