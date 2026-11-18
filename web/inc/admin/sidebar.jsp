<%@page pageEncoding="UTF-8"%>
<aside class="admin-sidebar">
    <a class="admin-brand" href="${ctx}/admin"><span class="admin-brand-icon"><svg><use href="#i-camera"/></svg></span><span>LensCam<small>ADMIN CONSOLE</small></span></a>
    <p class="sidebar-caption">HỆ THỐNG QUẢN LÝ</p>
    <nav aria-label="Điều hướng quản trị">
        <a class="${adminSection eq 'overview' ? 'active' : ''}" href="${ctx}/admin" aria-current="${adminSection eq 'overview' ? 'page' : 'false'}"><svg><use href="#i-grid"/></svg>Tổng quan đơn thuê</a>
        <a class="${empty adminSection or adminSection eq 'rentals' ? 'active' : ''}" href="${ctx}/admin/rentals" aria-current="${empty adminSection or adminSection eq 'rentals' ? 'page' : 'false'}"><svg><use href="#i-list"/></svg>Quản lý đơn thuê<c:if test="${not empty counts['pending']}"><span class="sidebar-count">${counts['pending']}</span></c:if></a>
        <a class="${adminSection eq 'devices' ? 'active' : ''}" href="${ctx}/admin/devices" aria-current="${adminSection eq 'devices' ? 'page' : 'false'}"><svg><use href="#i-settings"/></svg>Quản lý thiết bị</a>
        <a class="${adminSection eq 'users' ? 'active' : ''}" href="${ctx}/admin/users" aria-current="${adminSection eq 'users' ? 'page' : 'false'}"><svg><use href="#i-users"/></svg>Quản lý người dùng</a>
        <a class="${adminSection eq 'workflow' ? 'active' : ''}" href="${ctx}/admin/workflow" aria-current="${adminSection eq 'workflow' ? 'page' : 'false'}"><svg><use href="#i-check"/></svg>Quy trình vận hành</a>
    </nav>
    <div class="sidebar-bottom"><span class="status-dot"></span>KHU VỰC QUẢN TRỊ<small>LensCam Rental Operations</small></div>
</aside>
