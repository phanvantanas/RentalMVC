<%@page pageEncoding="UTF-8"%>
<section class="user-filter-panel" aria-label="Lọc người dùng">
    <div class="user-filter-row">
        <label class="user-search">
            <svg aria-hidden="true">
                <use href="#i-search" />
            </svg>
            <input id="user-search" type="search" placeholder="Tìm theo tên, email, số điện thoại…"
                aria-label="Tìm người dùng">
        </label>
        <label>
            <span class="sr-only">Vai trò</span>
            <select id="user-role" aria-label="Lọc vai trò">
                <option value="">Tất cả vai trò</option>
                <option value="user">Khách hàng</option>
                <option value="admin">Quản trị viên</option>
            </select>
        </label>
        <label>
            <span class="sr-only">Trạng thái</span>
            <select id="user-status" aria-label="Lọc trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="active">Hoạt động</option>
                <option value="locked">Đã khóa</option>
            </select>
        </label>
        <label>
            <span class="sr-only">Lịch sử thuê</span>
            <select id="user-rentals" aria-label="Lọc lịch sử thuê">
                <option value="">Tất cả lịch sử thuê</option>
                <option value="yes">Đã có đơn thuê</option>
                <option value="no">Chưa có đơn thuê</option>
            </select>
        </label>
    </div>
    <div class="user-bulk-bar">
        <div>
            <span id="user-selection-count" aria-live="polite">Đã chọn: 0</span>
            <button type="button" id="clear-user-selection">Bỏ chọn tất cả</button>
        </div>
        <div>
            <button type="button" data-user-bulk="unlock" disabled>
                <svg aria-hidden="true">
                    <use href="#i-unlock" />
                </svg>Mở khóa
            </button>
            <button type="button" data-user-bulk="lock" disabled>
                <svg aria-hidden="true">
                    <use href="#i-lock" />
                </svg>Khóa
            </button>
            <button type="button" data-user-bulk="delete" class="user-delete-button" disabled>
                <svg aria-hidden="true">
                    <use href="#i-trash" />
                </svg>Xóa
            </button>
        </div>
    </div>
</section>
<section class="user-table-panel" aria-label="Danh sách người dùng">
    <div class="user-table-scroll">
        <table class="user-table">
            <thead>
                <tr>
                    <th class="user-check">
                        <input type="checkbox" id="select-user-page" aria-label="Chọn người dùng trên trang">
                    </th>
                    <th>NGƯỜI DÙNG</th>
                    <th>SỐ ĐIỆN THOẠI</th>
                    <th>VAI TRÒ</th>
                    <th>ĐƠN THUÊ / TIỀN ĐÃ THU</th>
                    <th>TRẠNG THÁI</th>
                    <th>THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="u" items="${users}">
                    <tr class="user-row" data-id="${u.id}" data-name="<c:out value='${u.name}'/>"
                        data-email="<c:out value='${u.email}'/>" data-phone="<c:out value='${u.phone}'/>"
                        data-role="<c:out value='${u.role}'/>" data-status="<c:out value='${u.status}'/>"
                        data-orders="${u.rental_count}" data-revenue="${u.revenue}" data-self="${u.id eq sessionScope.user.id}">
                        <td class="user-check">
                            <input type="checkbox" class="user-checkbox" aria-label="Chọn tài khoản ${u.id}">
                        </td>
                        <td>
                            <div class="user-profile">
                                <span class="user-initials" aria-hidden="true"></span>
                                <div>
                                    <strong>
                                        <c:out value="${u.name}" />
                                        <c:if test="${u.id eq sessionScope.user.id}">
                                            <span class="user-self">Bạn</span>
                                        </c:if>
                                    </strong>
                                    <small>
                                        <c:out value="${u.email}" />
                                    </small>
                                    <span class="user-joined">Tham gia
                                        <fmt:formatDate value="${u.created_at}" pattern="dd/MM/yyyy" />
                                    </span>
                                </div>
                            </div>
                        </td>
                        <td>
                            <c:out value="${u.phone}" default="Chưa cập nhật" />
                        </td>
                        <td>
                            <span class="user-role-badge ${u.role}">${u.role eq 'admin' ? 'Quản trị viên' : 'Khách hàng'}</span>
                        </td>
                        <td>
                            <strong class="user-rental-count">${u.rental_count} đơn</strong>
                            <small class="user-revenue">
                                <fmt:formatNumber value="${u.revenue}" maxFractionDigits="0" />đ
                            </small>
                        </td>
                        <td>
                            <span class="user-state ${u.status}">
                                <span></span>${u.status eq 'locked' ? 'Đã khóa' : 'Hoạt động'}
                            </span>
                        </td>
                        <td>
                            <div class="user-row-actions">
                                <button type="button" data-edit-user="${u.id}"
                                    aria-label="Sửa tài khoản ${u.id}" title="Sửa thông tin">
                                    <svg>
                                        <use href="#i-edit" />
                                    </svg>
                                </button>
                                <button type="button" data-toggle-user="${u.id}"
                                    aria-label="${u.status eq 'locked' ? 'Mở khóa' : 'Khóa'} tài khoản ${u.id}" title="${u.status eq 'locked' ? 'Mở khóa' : 'Khóa'}"
                                    ${u.id eq sessionScope.user.id or (u.role eq 'admin' and u.status ne 'locked') ? 'disabled' : ''}>
                                    <svg>
                                        <use href="#i-${u.status eq 'locked' ? 'unlock' : 'lock'}" />
                                    </svg>
                                </button>
                                <button type="button" data-delete-user="${u.id}"
                                    aria-label="Xóa tài khoản ${u.id}"
                                    title="Chỉ xóa tài khoản khách hàng chưa có đơn thuê" ${u.role eq 'admin' or u.rental_count gt 0 ? 'disabled' : ''}>
                                    <svg>
                                        <use href="#i-trash" />
                                    </svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="user-empty" id="user-empty" hidden>
        <svg aria-hidden="true">
            <use href="#i-users" />
        </svg>
        <strong>Không tìm thấy người dùng</strong>
        <p>Thử thay đổi từ khóa hoặc điều kiện lọc.</p>
    </div>
    <div class="user-pagination">
        <span id="user-page-label"></span>
        <div>
            <label>Mỗi trang <select id="user-page-size">
                    <option value="10">10</option>
                    <option value="20">20</option>
                    <option value="50">50</option>
                </select>
            </label>
            <button type="button" id="user-previous" aria-label="Trang trước">←</button>
            <span id="user-page-number"></span>
            <button type="button" id="user-next" aria-label="Trang sau">→</button>
        </div>
    </div>
</section>
<p class="user-data-note">Tiền đã thu chỉ tính thanh toán được xác nhận, không gồm đơn hủy. Tài khoản có đơn thuê được
    giữ lại để bảo toàn lịch sử.</p>
