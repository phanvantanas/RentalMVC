<%@page pageEncoding="UTF-8"%>
<dialog class="rental-confirm-dialog" id="rental-confirm-dialog" aria-labelledby="rental-confirm-title"
    aria-describedby="rental-confirm-description">
    <button type="button" class="rental-confirm-close" data-cancel-rental-confirm aria-label="Đóng">×</button>
    <span class="rental-confirm-icon" aria-hidden="true">
        <svg>
            <use href="#i-check" />
        </svg>
    </span>
    <span class="eyebrow">XÁC NHẬN THAO TÁC</span>
    <h2 id="rental-confirm-title">Xác nhận đơn thuê</h2>
    <p class="rental-confirm-order" id="rental-confirm-order"></p>
    <p id="rental-confirm-description"></p>
    <div class="rental-confirm-actions">
        <button type="button" class="button secondary" data-cancel-rental-confirm autofocus>Quay lại</button>
        <button type="button" class="button" id="rental-confirm-submit">Xác nhận</button>
    </div>
</dialog>
