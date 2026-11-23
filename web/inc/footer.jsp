<%@page pageEncoding="UTF-8"%>
<footer class="rental-footer">
    <div class="container">
        <div class="footer-columns">
            <section class="footer-about" aria-label="Giới thiệu LensCam">
                <div class="footer-logo-row">
                    <a class="footer-logo" href="${ctx}/home">LensCam</a>
                    <span class="footer-fleet-badge">PRO GEAR RENTAL</span>
                </div>
                <p>Nền tảng kiểm định, cho thuê máy ảnh kỹ thuật số, ống kính rạp chiếu phim và trang thiết bị trường
                    quay chuyên nghiệp chuẩn cine tại Việt Nam.</p>
                <ul class="footer-highlights">
                    <li>
                        <svg aria-hidden="true">
                            <use href="#i-check" />
                        </svg>Thiết bị &amp; lịch trống được cập nhật theo ngày
                    </li>
                    <li>
                        <svg aria-hidden="true">
                            <use href="#i-check" />
                        </svg>Giá thuê minh bạch, lựa chọn thiết bị linh hoạt
                    </li>
                </ul>
            </section>
            <section aria-labelledby="footer-locations">
                <h2 id="footer-locations">Hệ thống Hub Nhận Máy</h2>
                <div class="footer-location">
                    <h3>Hub Huế:</h3>
                    <address>77 Nguyễn Huệ,P.Thuận Hóa,TP Huế</address>
                    <p class="footer-hours">08:00 - 21:00 (Hằng ngày)</p>
                </div>
                <div class="footer-location">
                    <h3>Hub Đà Nẵng:</h3>
                    <address>252 Nguyễn Tất Thành,Hải Châu,TP Đà Nẵng</address>
                    <p class="footer-hours">07:30 - 22:00 (Hằng ngày)</p>
                </div>
            </section>
            <section aria-labelledby="footer-support">
                <h2 id="footer-support">Chính Sách &amp; Hỗ Trợ</h2>
                <ul class="footer-links">
                    <li>
                        <button type="button" data-open="guide-dialog">Quy trình thuê &amp; nhận thiết bị</button>
                    </li>
                    <li>
                        <button type="button" data-open="guide-dialog">Bảng giá &amp; thông tin đặt cọc</button>
                    </li>
                    <li>
                        <a href="#search-form">Tra cứu lịch thiết bị còn trống</a>
                    </li>
                    <li>
                        <button type="button" data-open="history-dialog">Theo dõi lịch thuê của bạn</button>
                    </li>
                    <li>
                        <a href="#catalog">Khám phá thiết bị &amp; ưu đãi thuê</a>
                    </li>
                </ul>
            </section>
            <section aria-labelledby="footer-payments">
                <h2 id="footer-payments">Thanh Toán &amp; Ký Quỹ</h2>
                <p>Tiền thuê được tính theo ngày. Vui lòng xác nhận phương thức thanh toán và tiền cọc khi làm thủ tục
                    nhận máy.</p>
                <div class="footer-payment-tags">
                    <span>TIỀN THUÊ</span>
                    <span>TIỀN CỌC</span>
                    <span>ƯU ĐÃI DÀI NGÀY</span>
                </div>
                <p class="footer-payment-note">
                    <svg aria-hidden="true">
                        <use href="#i-check" />
                    </svg>Kiểm tra chi phí trước khi thuê
                </p>
            </section>
        </div>
        <div class="footer-bottom">
            <p>©
                <%= java.time.Year.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) %> LensCam Pro Gear Rental Platform. All rights reserved.
            </p>
            <nav aria-label="Liên kết chân trang">
                <button type="button" data-open="guide-dialog">Hướng dẫn
                    thuê</button>
                <button type="button" data-open="history-dialog">Lịch thuê của tôi</button>
                <a href="#search-form">Chọn lịch thuê ↑</a>
            </nav>
        </div>
    </div>
</footer>
