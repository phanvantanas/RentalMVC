<%@page pageEncoding="UTF-8"%>
<div class="catalog-controls">
    <div class="container catalog-controls-inner">
        <div class="catalog-sort"><span class="catalog-control-label" id="sort-heading">SẮP XẾP:</span><div class="sort-segments" role="group" aria-labelledby="sort-heading">
            <button type="button" data-sort="name" aria-pressed="false">TÊN A-Z</button>
            <button type="button" data-sort="price-asc" aria-pressed="false">GIÁ THẤP</button>
            <button type="button" data-sort="price-desc" aria-pressed="false">GIÁ CAO</button>
            <button type="button" data-sort="brand" class="active" aria-pressed="true">THƯƠNG HIỆU</button>
        </div></div>
        <div class="catalog-view" role="group" aria-label="Chế độ xem thiết bị"><span class="catalog-control-label">XEM:</span><button type="button" class="active" data-view="grid" aria-label="Xem dạng lưới" aria-pressed="true"><svg aria-hidden="true"><use href="#i-grid"/></svg></button><button type="button" data-view="list" aria-label="Xem dạng danh sách" aria-pressed="false"><svg aria-hidden="true"><use href="#i-list"/></svg></button></div>
    </div>
</div>
