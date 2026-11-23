<%@page pageEncoding="UTF-8"%>
<dialog class="user-dialog" id="user-editor" aria-labelledby="user-editor-title" data-reopen="${not empty userDraft}">
    <form method="dialog">
        <button class="user-dialog-close" aria-label="Đóng">×</button>
    </form>
    <span class="eyebrow">LENSCAM ACCOUNT MANAGEMENT</span>
    <h2 id="user-editor-title">Thêm người dùng mới</h2>
    <c:if test="${not empty userDraft and not empty userError}">
        <p class="notice error" role="alert">
            <c:out value="${userError}" />
        </p>
    </c:if>
    <form action="${ctx}/admin/users" method="post" class="user-editor-form">
        <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
        <input type="hidden" name="action" id="user-editor-action" value="<c:out value='${empty userDraft ? "create" : userDraft.action}'/>">
        <input type="hidden" name="id" id="user-editor-id" value="<c:out value='${userDraft.id}'/>">
        <label>Họ và tên *
            <input id="user-editor-name" name="name" maxlength="100" value="<c:out value='${userDraft.name}'/>"
                autocomplete="name" placeholder="Ví dụ: Nguyễn Văn An" required>
        </label>
        <div class="user-form-row">
            <label>Email *
                <input type="email" id="user-editor-email" name="email" maxlength="150"
                    value="<c:out value='${userDraft.email}'/>" autocomplete="email" placeholder="email@domain.com" required>
            </label>
            <label>Số điện thoại
                <input type="tel" id="user-editor-phone" name="phone" maxlength="11" pattern="[0-9]{9,11}"
                    value="<c:out value='${userDraft.phone}'/>" autocomplete="tel" placeholder="09xxxxxxxx">
            </label>
        </div>
        <div class="user-form-row">
            <label>Vai trò *<select id="user-editor-role" name="role">
                    <option value="user" ${userDraft.role eq 'user' ? 'selected' : ''}>Khách hàng</option>
                    <option value="admin" ${userDraft.role eq 'admin' ? 'selected' : ''}>Quản trị viên</option>
                </select>
            </label>
            <label>Trạng thái *<select id="user-editor-status" name="status">
                    <option value="active" ${userDraft.status eq 'active' ? 'selected' : ''}>Hoạt động</option>
                    <option value="locked" ${userDraft.status eq 'locked' ? 'selected' : ''}>Đã khóa</option>
                </select>
            </label>
        </div>
        <div id="user-password-fields" class="user-form-row">
            <label>Mật khẩu ban đầu *
                <input id="user-editor-password" type="password" name="password" minlength="8" maxlength="128"
                    autocomplete="new-password" placeholder="Từ 8 đến 128 ký tự" required>
            </label>
            <label>Nhập lại mật khẩu *
                <input id="user-editor-confirm" type="password" name="confirm" minlength="8" maxlength="128"
                    autocomplete="new-password" required>
            </label>
        </div>
        <p class="user-form-note" id="user-editor-note">Tài khoản quản trị viên có quyền truy cập và quản lý dữ liệu
            trong khu vực admin.</p>
        <button class="button" type="submit">Lưu người dùng</button>
    </form>
</dialog>
<dialog class="user-dialog user-confirm-dialog" id="user-action-dialog" aria-labelledby="user-action-title">
    <form method="dialog">
        <button class="user-dialog-close" aria-label="Đóng">×</button>
    </form>
    <span class="eyebrow">THAO TÁC TÀI KHOẢN</span>
    <h2 id="user-action-title"></h2>
    <p id="user-action-description"></p>
    <form action="${ctx}/admin/users" method="post" class="user-action-form">
        <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
        <input type="hidden" name="action" id="user-bulk-action">
        <div id="user-selected-ids"></div>
        <p class="user-form-note" id="user-action-note"></p>
        <button class="button" id="user-action-confirm" type="submit">Xác nhận</button>
    </form>
</dialog>
