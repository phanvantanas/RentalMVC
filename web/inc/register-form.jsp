<%@page pageEncoding="UTF-8"%>
<section class="auth-card register-card" aria-labelledby="register-title">
    <h1 id="register-title">Tạo tài khoản LensCam</h1>
    <c:if test="${not empty requestScope.error_register}">
        <p class="auth-message auth-error" role="alert">
            <c:out value="${requestScope.error_register}" />
        </p>
    </c:if>
    <form id="register-form" action="${ctx}/register" method="post">
        <label class="register-label" for="register-name">Họ và tên</label>
        <div class="auth-input">
            <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M9 5H4v16h16V5h-5M9 3h6v5H9z" />
                <circle cx="12" cy="12" r="2" />
                <path d="M8 18c0-4 8-4 8 0" />
            </svg>
            <input id="register-name" type="text" name="name" placeholder="Ví dụ: Nguyễn Văn An"
                value="<c:out value='${param.name}'/>" autocomplete="name" maxlength="150" required>
        </div>
        <div class="register-contact-grid">
            <div>
                <label class="register-label" for="register-phone">Số điện thoại <span>SỐ LIÊN HỆ</span>
                </label>
                <div class="auth-input">
                    <svg aria-hidden="true" viewBox="0 0 24 24">
                        <rect x="7" y="2" width="10" height="20" rx="2" />
                        <path d="M11 18h2" />
                    </svg>
                    <input id="register-phone" type="tel" name="phone" placeholder="09xx xxx xxx"
                        value="<c:out value='${param.phone}'/>" autocomplete="tel" inputmode="tel" pattern="[0-9]{9,11}"
                        title="Số điện thoại gồm 9 đến 11 chữ số" maxlength="11" required>
                </div>
            </div>
            <div>
                <label class="register-label" for="register-email">Email</label>
                <div class="auth-input">
                    <svg aria-hidden="true" viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="4" />
                        <path d="M16 8v6a2 2 0 0 0 4 0v-2a8 8 0 1 0-3 6.2" />
                    </svg>
                    <input id="register-email" type="email" name="email" placeholder="an.nguyen@email.com"
                        value="<c:out value='${param.email}'/>" autocomplete="email" autocapitalize="none"
                        spellcheck="false" required>
                </div>
            </div>
        </div>
        <label class="register-label" for="register-password">Mật khẩu <span>TỐI THIỂU 8 KÝ TỰ</span>
        </label>
        <div class="auth-input register-password-input">
            <svg aria-hidden="true" viewBox="0 0 24 24">
                <rect x="5" y="10" width="14" height="11" rx="2" />
                <path d="M8 10V6a4 4 0 0 1 8 0v4m-4 5v2" />
            </svg>
            <input id="register-password" type="password" name="password" placeholder="Nhập mật khẩu của bạn"
                autocomplete="new-password" minlength="8" maxlength="128" aria-describedby="password-strength-label"
                required>
            <button id="register-toggle" type="button" class="password-toggle" aria-label="Hiện mật khẩu"
                aria-pressed="false">
                <svg aria-hidden="true" viewBox="0 0 24 24">
                    <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z" />
                    <circle cx="12" cy="12" r="3" />
                    <path class="eye-slash" d="m3 3 18 18" />
                </svg>
            </button>
        </div>
        <div class="password-strength" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
            <span></span>
        </div>
        <p class="password-strength-label" id="password-strength-label" aria-live="polite"></p>
        <label class="register-label" for="register-confirm">Nhập lại mật khẩu</label>
        <div class="auth-input">
            <svg aria-hidden="true">
                <use href="#i-check" />
            </svg>
            <input id="register-confirm" type="password" name="confirm" placeholder="Nhập lại mật khẩu"
                autocomplete="new-password" minlength="8" maxlength="128" required>
        </div>
        <label class="register-agreement">
            <input type="checkbox" id="register-confirm-info" required>
            <span>Tôi xác nhận
                các thông tin đăng ký trên là chính xác.</span>
        </label>
        <button class="auth-submit register-submit" type="submit">
            <span>Đăng ký tài khoản</span>
            <svg aria-hidden="true" viewBox="0 0 24 24">
                <circle cx="9" cy="7" r="3" />
                <path d="M3 20v-2a6 6 0 0 1 12 0v2zm16-9v6m-3-3h6" />
            </svg>
        </button>
    </form>
    <p class="register-login">Đã có tài khoản? <a href="${ctx}/login">Đăng nhập ngay</a>
    </p>
    <p class="register-note">
        <svg aria-hidden="true">
            <use href="#i-check" />
        </svg>SẴN SÀNG CHO NHỮNG KHUNG HÌNH TIẾP THEO
    </p>
</section>
