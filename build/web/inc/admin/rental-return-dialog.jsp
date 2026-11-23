<%@page pageEncoding="UTF-8"%>
<dialog id="rental-return-dialog" class="rental-return-dialog" aria-labelledby="rental-return-title">
    <button type="button" class="return-dialog-close" id="close-rental-return" aria-label="Đóng">×</button>
    <span class="eyebrow">NHẬN THIẾT BỊ &amp; PHÍ TRẢ TRỄ</span>
    <h2 id="rental-return-title">Xác nhận nhận lại máy</h2>
    <p class="return-order-code" id="return-order-code"></p>
    <form id="rental-return-form" action="${ctx}/admin/rentals" method="post">
        <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
        <input type="hidden" name="action" value="return_rental">
        <input type="hidden" name="id" id="return-rental-id">
        <div class="return-schedule">
            <div>
                <span>Ngày hẹn trả</span>
                <strong id="return-due-date"></strong>
            </div>
            <div>
                <span>Tổng giá thuê / ngày trong đơn</span>
                <strong id="return-daily-rate"></strong>
            </div>
        </div>
        <div class="return-fields">
            <label>Giờ hẹn trả *
                <input type="time" id="return-due-time" name="legacy_due_time" required>
            </label>
            <label>Thời điểm trả thực tế (giờ Việt Nam) *
                <input type="datetime-local" name="actual_return" id="return-actual-time" required>
            </label>
        </div>
        <p class="return-legacy-note" id="return-legacy-note" hidden>Đơn cũ chưa lưu giờ trả. Đang tạm chọn 17:00; hãy
            đối chiếu giờ hẹn với khách trước khi xác nhận.</p>
        <div class="return-fee-preview" aria-live="polite">
            <div>
                <span>Trả trễ</span>
                <strong id="return-delay">—</strong>
            </div>
            <div>
                <span>Phí trả trễ dự kiến</span>
                <strong id="return-calculated-fee">—</strong>
            </div>
        </div>
        <label class="return-check">
            <input type="checkbox" id="return-adjust-fee">Giảm / miễn phí trả trễ
        </label>
        <div id="return-adjust-fields" hidden>
            <label>Phí thực thu (VNĐ)
                <input type="number" name="adjusted_fee" id="return-adjusted-fee" min="0" step="1" disabled>
            </label>
            <label>Lý do giảm / miễn phí (khách có
                thể xem)
                <textarea name="adjustment_reason" id="return-adjustment-reason" maxlength="500" rows="2" disabled
                    placeholder="Ví dụ: Khách đã báo trước, không ảnh hưởng lịch thuê tiếp theo"></textarea>
            </label>
        </div>
        <label class="return-check">
            <input type="checkbox" name="fee_collected" value="1" id="return-fee-collected">Đã
            thu đủ phí trả trễ
        </label>
        <p class="return-help">Chỉ chọn khi đã thực nhận tiền mặt hoặc chuyển khoản phí trễ. Tiền thuê đã thanh toán
            được giữ nguyên.</p>
        <details class="return-policy">
            <summary>Cách tính phí trả trễ</summary>
            <table>
                <tbody>
                    <tr>
                        <td>Tối đa 1 giờ</td>
                        <td>Miễn phí</td>
                    </tr>
                    <tr>
                        <td>Trên 1–3 giờ</td>
                        <td>10% giá/ngày</td>
                    </tr>
                    <tr>
                        <td>Trên 3–6 giờ</td>
                        <td>20% giá/ngày</td>
                    </tr>
                    <tr>
                        <td>Trên 6–12 giờ</td>
                        <td>40% giá/ngày</td>
                    </tr>
                    <tr>
                        <td>Trên 12–24 giờ</td>
                        <td>70% giá/ngày</td>
                    </tr>
                </tbody>
            </table>
            <p>Mỗi 24 giờ trễ tính 70%; phần giờ dư áp dụng lại bảng trên. Ví dụ trễ 25 giờ tính 70%, trễ 26 giờ tính
                80%. Tính trên tổng giá thuê/ngày của các thiết bị trong đơn, làm tròn đến đồng.</p>
        </details>
        <div class="return-form-actions">
            <button type="button" class="button secondary" id="cancel-rental-return">Quay
                lại</button>
            <button type="submit" class="button" id="save-rental-return">Xác nhận nhận máy &amp;
                phí</button>
        </div>
    </form>
</dialog>
