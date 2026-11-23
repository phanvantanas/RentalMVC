<%@page pageEncoding="UTF-8"%>
<section class="stat-grid" id="overview" aria-label="Tổng quan đơn thuê">
    <button class="stat-card" type="button" data-quick-status="pending">
        <span>
            <span class="stat-label">Chờ
                duyệt</span>
            <strong class="stat-red">
                <fmt:formatNumber value="${empty counts['pending'] ? 0 : counts['pending']}" pattern="00" />
            </strong>
            <small>
                <span class="status-dot red"></span>Cần kiểm tra &amp; xác nhận
            </small>
        </span>
        <span class="stat-icon red">
            <svg>
                <use href="#i-list" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-quick-status="confirmed">
        <span>
            <span class="stat-label">Chờ giao
                máy</span>
            <strong>
                <fmt:formatNumber value="${empty counts['confirmed'] ? 0 : counts['confirmed']}" pattern="00" />
            </strong>
            <small>
                <span class="status-dot"></span>Đã duyệt · Chờ bàn giao
            </small>
        </span>
        <span class="stat-icon">
            <svg>
                <use href="#i-calendar" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-quick-status="renting">
        <span>
            <span class="stat-label">Đang
                thuê</span>
            <strong>
                <fmt:formatNumber value="${empty counts['renting'] ? 0 : counts['renting']}" pattern="00" />
            </strong>
            <small>
                <span class="status-dot blue"></span>Thiết bị đang ở khách hàng
            </small>
        </span>
        <span class="stat-icon">
            <svg>
                <use href="#i-camera" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-quick-status="overdue">
        <span>
            <span class="stat-label">Quá hạn
                trả</span>
            <strong class="stat-amber">
                <fmt:formatNumber value="${empty counts['overdue'] ? 0 : counts['overdue']}" pattern="00" />
            </strong>
            <small>
                <span class="status-dot amber"></span>Cần theo dõi lịch trả máy
            </small>
        </span>
        <span class="stat-icon amber">
            <svg>
                <use href="#i-clock" />
            </svg>
        </span>
    </button>
</section>
