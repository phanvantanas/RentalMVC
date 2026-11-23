<%@page pageEncoding="UTF-8"%>
<dialog id="history-dialog" class="modal history-modal">
    <form method="dialog">
        <button class="close-dialog" aria-label="Đóng">×</button>
    </form>
    <span class="eyebrow">TÀI
        KHOẢN CỦA BẠN</span>
    <h2>Lịch thuê của tôi</h2>
    <c:choose>
        <c:when test="${empty sessionScope.user}">
            <p class="muted">Đăng nhập để xem lịch thuê của bạn.</p>
            <a class="button" href="${ctx}/login">Đăng
                nhập</a>
        </c:when>
        <c:when test="${empty myRentals}">
            <p class="muted">Bạn chưa có lịch thuê nào.</p>
        </c:when>
        <c:otherwise>
            <div class="history-scroll">
                <table>
                    <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Ngày nhận</th>
                            <th>Ngày trả</th>
                            <th>Trạng thái</th>
                            <th>Tiền thuê</th>
                            <th>Thanh toán</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${myRentals}" var="r">
                            <tr>
                                <td>#${r.id}</td>
                                <td>
                                    <fmt:formatDate value="${r.startDate}" pattern="dd/MM/yyyy" />
                                </td>
                                <td>
                                    <fmt:formatDate value="${r.endDate}" pattern="dd/MM/yyyy" />
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.status eq 'pending'}">Chờ xác nhận</c:when>
                                        <c:when test="${r.status eq 'confirmed'}">Đã xác nhận</c:when>
                                        <c:when test="${r.status eq 'renting'}">Đang thuê</c:when>
                                        <c:when test="${r.status eq 'returned'}">Đã trả</c:when>
                                        <c:when test="${r.status eq 'cancelled'}">Đã hủy</c:when>
                                        <c:otherwise>
                                            <c:out value="${r.status}" />
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <fmt:formatNumber value="${r.total}" maxFractionDigits="0" />đ
                                </td>
                                <td>
                                    <c:if test="${not empty paymentOrderIds and paymentOrderIds.contains(r.id)}">
                                        <a href="${ctx}/checkout?order=${r.id}">Xem
                                            thanh toán ↗</a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</dialog>
