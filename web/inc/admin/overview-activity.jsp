<%@page pageEncoding="UTF-8"%>
<aside class="dashboard-panel activity-panel">
    <div class="dashboard-panel-heading">
        <h2>
            <span class="activity-heading-icon">◷</span> Đơn Thuê Mới Tạo
        </h2>
        <span class="live-dot" aria-hidden="true"></span>
    </div>
    <div class="activity-timeline">
        <c:forEach var="event" items="${dashboard.activity}">
            <article class="activity-event">
                <span class="activity-node ${event.status}">
                    <svg>
                        <use href="#i-list" />
                    </svg>
                </span>
                <div>
                    <a href="${ctx}/admin/rentals?order=${event.id}">
                        <strong>Đơn
                            #ORD-${event.id}</strong>
                    </a>
                    <time>
                        <fmt:formatDate value="${event.created_at}" pattern="dd/MM/yyyy HH:mm" />
                    </time>
                    <p>Khách hàng <b>
                            <c:out value="${event.name}" default="Chưa có tên" />
                        </b> đã tạo đơn thuê.</p>
                    <span class="status-badge ${event.status}">
                        <c:out value="${statusLabels[event.status]}" />
                    </span>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty dashboard.activity}">
            <p class="dashboard-empty">Chưa có đơn mới trong kỳ báo cáo.</p>
        </c:if>
    </div>
    <a class="activity-all" href="${ctx}/admin/rentals">Xem tất cả đơn thuê →</a>
    <p class="panel-footnote">Hiển thị tối đa 8 đơn gần nhất trong khoảng ngày đã chọn.</p>
</aside>
