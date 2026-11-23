<%@page pageEncoding="UTF-8"%>
<section class="dashboard-panel top-products">
    <div class="dashboard-panel-heading">
        <div>
            <h2>Thiết Bị Thuê Nhiều Nhất</h2>
            <p>Xếp hạng theo số thiết bị trong đơn được tạo trong kỳ, loại trừ đơn hủy.</p>
        </div>
        <a href="${ctx}/admin/devices">Kho thiết bị ↗</a>
    </div>
    <div class="overview-table-scroll">
        <table>
            <thead>
                <tr>
                    <th>THIẾT BỊ / ỐNG KÍNH</th>
                    <th>PHÂN LOẠI</th>
                    <th>ĐƠN / THIẾT BỊ</th>
                    <th>GIÁ TRỊ ĐƠN PHÂN BỔ</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="product" items="${dashboard.top}">
                    <tr>
                        <td>
                            <div class="top-product">
                                <c:if test="${not empty product.imageUrl}">
                                    <img src="<c:out value='${product.imageUrl}'/>" alt="" loading="lazy">
                                </c:if>
                                <div>
                                    <strong>
                                        <c:out value="${product.name}" />
                                    </strong>
                                    <small>Mã mẫu: #${product.id}</small>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="product-category">
                                <c:out value="${product.category}" default="Thiết bị" />
                            </span>
                        </td>
                        <td>
                            <b>${product.orders}</b> đơn<small>${product.units} thiết bị</small>
                        </td>
                        <td class="top-value">
                            <fmt:formatNumber value="${product.value_total}" maxFractionDigits="0" />đ
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty dashboard.top}">
                    <tr>
                        <td colspan="4" class="dashboard-empty">Chưa có thiết bị được thuê trong khoảng ngày này.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
    <p class="panel-footnote">Giá trị đơn được chia theo tỷ trọng giá thuê từng thiết bị; đây không phải số tiền đã thu.
    </p>
</section>
