<%@page pageEncoding="UTF-8"%>
<c:set var="returnRecord" value="${returnDetails[r.id]}" />
<c:if test="${not empty returnRecord}">
    <section class="return-audit">
        <h3 class="detail-subtitle">Nhận máy &amp; phí trả trễ</h3>
        <dl class="detail-fields">
            <div>
                <dt>Hẹn trả</dt>
                <dd>
                    <fmt:formatDate value="${returnRecord.due_at}" pattern="dd/MM/yyyy HH:mm" />
                </dd>
            </div>
            <div>
                <dt>Trả thực tế</dt>
                <dd>
                    <fmt:formatDate value="${returnRecord.actual_return_at}" pattern="dd/MM/yyyy HH:mm" />
                </dd>
            </div>
            <div>
                <dt>Trễ</dt>
                <dd>
                    <fmt:formatNumber value="${returnRecord.late_seconds div 3600}" maxFractionDigits="1" /> giờ
                </dd>
            </div>
            <div>
                <dt>Phí theo chính sách</dt>
                <dd>
                    <fmt:formatNumber value="${returnRecord.calculated_fee}" maxFractionDigits="0" />đ
                </dd>
            </div>
            <div>
                <dt>Phí thực thu</dt>
                <dd>
                    <fmt:formatNumber value="${returnRecord.final_fee}" maxFractionDigits="0" />đ
                </dd>
            </div>
            <div>
                <dt>Trạng thái phí</dt>
                <dd>${returnRecord.fee_status eq 'paid' ? 'Đã thu phí' : (returnRecord.fee_status eq 'waived' ? 'Không thu phí' : 'Chưa thu phí')}</dd>
            </div>
            <div>
                <dt>Người nhận máy</dt>
                <dd>
                    <c:out value="${returnRecord.receiver_name}" />
                </dd>
            </div>
        </dl>
        <c:if test="${not empty returnRecord.adjustment_reason}">
            <p class="return-adjustment-text">Giảm / miễn phí:
                <c:out value="${returnRecord.adjustment_reason}" />
            </p>
        </c:if>
        <c:if test="${returnRecord.fee_status eq 'unpaid' and returnRecord.final_fee gt 0}">
            <form class="status-action-form" method="post" action="${ctx}/admin/rentals">
                <input type="hidden" name="csrf" value="${sessionScope.adminCsrf}">
                <input type="hidden" name="id" value="${r.id}">
                <input type="hidden" name="action" value="confirm_late_fee">
                <button class="button green" type="submit">Xác
                    nhận đã thu phí trễ</button>
            </form>
        </c:if>
    </section>
</c:if>
