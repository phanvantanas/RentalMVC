<%@page pageEncoding="UTF-8"%>
<dialog class="inventory-dialog product-dialog" id="product-editor" aria-labelledby="product-editor-title">
<form method="dialog"><button class="inventory-close" aria-label="Đóng">×</button></form>
<span class="eyebrow">LENSCAM PRODUCT CATALOG</span><h2 id="product-editor-title">Thêm sản phẩm mới</h2><p class="muted">Tạo mẫu sản phẩm và nhập chiếc máy đầu tiên vào kho.</p>
<form class="inventory-form product-form" action="${ctx}/admin/devices" method="post">
<input type="hidden" name="csrf" value="${sessionScope.adminCsrf}"><input type="hidden" name="action" value="create_product">
<label>Mã sản phẩm<input value="Tự động tạo khi lưu" disabled></label>
<label>Tên sản phẩm *<input name="name" maxlength="150" required placeholder="Ví dụ: Sony Alpha A7 IV"></label>
<div class="product-form-row"><label>Danh mục *<select name="id_category" required><option value="">Chọn danh mục</option><c:forEach items="${categories}" var="cat"><option value="${cat.id}"><c:out value="${cat.name}"/></option></c:forEach></select></label><label>Giá thuê / ngày (VNĐ) *<input name="price" type="number" min="0" max="9999999999.99" step="0.01" required placeholder="Ví dụ: 450000"></label></div>
<label>Ảnh sản phẩm<select name="image"><option value="">Chưa chọn ảnh</option><c:forEach items="${productImageFiles}" var="image"><option value="<c:out value='${image}'/>"><c:out value="${image}"/></option></c:forEach></select><small>Chọn ảnh có sẵn trong thư mục assets/images.</small></label>
<label>Mô tả<textarea name="description" rows="4" maxlength="16000" placeholder="Thông số, tính năng và phụ kiện đi kèm…"></textarea></label>
<label>Trạng thái sản phẩm *<select name="product_status" required><option value="active">Đang cho thuê</option><option value="inactive">Ngừng cho thuê</option></select></label>
<h3 class="product-device-heading">Thiết bị vật lý đầu tiên</h3>
<div class="product-form-row"><label>Số serial *<input name="serial" maxlength="100" required autocomplete="off" placeholder="Ví dụ: CAM-SN-0001"></label><label>Trạng thái thiết bị *<select name="status" required><option value="available">Sẵn sàng</option><option value="maintenance">Bảo trì / Kiểm tra</option></select></label></div>
<p class="inventory-form-note">Dấu * là trường bắt buộc. Mỗi chiếc máy có serial riêng; dùng “Nhập thêm thiết bị” để bổ sung máy cùng mẫu.</p>
<button class="button" type="submit">Lưu sản phẩm và thiết bị</button></form></dialog>
