<%@page pageEncoding="UTF-8"%>
<section class="stat-grid device-stats" aria-label="Tổng quan kho máy">
    <button class="stat-card" type="button" data-device-quick="">
        <span>
            <span class="stat-label">TỔNG THIẾT
                BỊ</span>
            <strong>${fn:length(devices)}</strong>
            <span class="device-stat-bar">
                <i style="width:100%"></i>
            </span>
            <small>Quản lý theo serial &amp; mẫu máy</small>
        </span>
        <span class="stat-icon">
            <svg>
                <use href="#i-list" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-device-quick="available">
        <span>
            <span class="stat-label">SẴN SÀNG CHO
                THUÊ</span>
            <strong class="stat-green">${empty deviceCounts['available'] ? 0 : deviceCounts['available']}</strong>
            <span class="device-stat-bar">
                <i class="green-bar" style="width:${empty devices ? 0 : (empty deviceCounts['available'] ? 0 : deviceCounts['available']) * 100 / fn:length(devices)}%"></i>
            </span>
            <small>Thiết bị có thể bố trí hôm
                nay</small>
        </span>
        <span class="stat-icon green-icon">
            <svg>
                <use href="#i-check" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-device-quick="renting">
        <span>
            <span class="stat-label">ĐANG CHO
                THUÊ</span>
            <strong>${empty deviceCounts['renting'] ? 0 : deviceCounts['renting']}</strong>
            <span class="device-stat-bar">
                <i style="width:${empty devices ? 0 : (empty deviceCounts['renting'] ? 0 : deviceCounts['renting']) * 100 / fn:length(devices)}%"></i>
            </span>
            <small>Thiết bị trong đơn đang thuê</small>
        </span>
        <span class="stat-icon">
            <svg>
                <use href="#i-calendar" />
            </svg>
        </span>
    </button>
    <button class="stat-card" type="button" data-device-quick="maintenance">
        <span>
            <span class="stat-label">BẢO TRÌ /
                KIỂM TRA</span>
            <strong class="stat-red">${empty deviceCounts['maintenance'] ? 0 : deviceCounts['maintenance']}</strong>
            <span class="device-stat-bar">
                <i class="red-bar" style="width:${empty devices ? 0 : (empty deviceCounts['maintenance'] ? 0 : deviceCounts['maintenance']) * 100 / fn:length(devices)}%"></i>
            </span>
            <small>Cần kiểm tra trước khi cho
                thuê</small>
        </span>
        <span class="stat-icon red">
            <svg>
                <use href="#i-settings" />
            </svg>
        </span>
    </button>
</section>
