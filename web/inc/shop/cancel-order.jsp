<%@page pageEncoding="UTF-8"%>
<details class="cancel-rental">
    <summary>Hủy đơn thuê</summary>
    <p>Bạn chắc chắn muốn hủy đơn #ORD-${cancelOrderId}? Máy sẽ được nhả cho khách khác. Chỉ hủy nếu bạn chưa chuyển tiền.
    </p>
    <form action="${ctx}/my-rentals" method="post">
        <input type="hidden" name="csrf" value="${sessionScope.shopCsrf}">
        <input type="hidden" name="action" value="cancel">
        <input type="hidden" name="order" value="${cancelOrderId}">
        <button type="submit">Xác nhận hủy đơn</button>
    </form>
</details>
