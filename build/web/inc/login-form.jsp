<%@page pageEncoding="UTF-8"%>
<section class="auth-card" aria-labelledby="login-title">
    <header class="auth-card-header">
        <a class="auth-brand" href="${ctx}/home" aria-label="LensCam trang chủ"><span class="auth-brand-icon"><svg><use href="#i-camera"/></svg></span><span><span class="auth-brand-name">LENSCAM <small>RENTAL FLEET</small></span><span class="auth-brand-caption">CINEMA PRECISION SYSTEM</span></span></a>
        <h1 id="login-title">Đăng nhập LensCam</h1>
    </header>
    <div class="auth-divider"></div>
    <c:if test="${param.account eq 'unavailable'}"><p class="auth-message auth-error" role="alert">Tài khoản đã bị khóa hoặc không còn tồn tại. Vui lòng liên hệ quản trị viên.</p></c:if>
    <c:if test="${not empty sessionScope.error_login}"><p class="auth-message auth-error" role="alert"><c:out value="${sessionScope.error_login}"/></p></c:if>
    <c:if test="${not empty sessionScope.success_register}"><p class="auth-message auth-success" role="status"><c:out value="${sessionScope.success_register}"/></p></c:if>
    <form id="login-form" action="${ctx}/login" method="post">
        <label class="auth-label" for="emailphone">SỐ ĐIỆN THOẠI HOẶC EMAIL</label>
        <div class="auth-input"><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M16 8v6a2 2 0 0 0 4 0v-2a8 8 0 1 0-3 6.2"/></svg><input type="text" name="emailphone" id="emailphone" value="<c:out value='${loginIdentifier}'/>" placeholder="0912 345 678 hoặc email@domain.com" autocomplete="username" autocapitalize="none" spellcheck="false" required></div>
        <label class="auth-label" for="password">MẬT KHẨU TRUY CẬP</label>
        <div class="auth-input"><svg aria-hidden="true" viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4m-4 5v2"/></svg><input type="password" name="password" id="password" placeholder="Nhập mật khẩu của bạn" autocomplete="current-password" required><button type="button" id="toggle-password" class="password-toggle" aria-label="Hiện mật khẩu" aria-pressed="false"><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/><path class="eye-slash" d="m3 3 18 18"/></svg></button></div>
        <div class="auth-options"><label><input type="checkbox" id="remember-account">Ghi nhớ email / số điện thoại</label><button type="button" data-auth-dialog="help-dialog">Quên mật khẩu?</button></div>
        <p class="remember-hint" id="remember-hint" hidden>Chỉ ghi nhớ email hoặc số điện thoại trên trình duyệt này.</p>
        <button type="submit" class="auth-submit"><span>Đăng nhập</span><svg><use href="#i-arrow"/></svg></button>
    </form>
    <div class="auth-or"><span>HOẶC TIẾP TỤC VỚI</span></div>
    <div class="auth-social"><button type="button" disabled><span class="google-mark" aria-hidden="true">G</span><span>Google <small>Sắp có</small></span></button><button type="button" disabled><svg aria-hidden="true" viewBox="0 0 24 24" class="apple-mark"><path d="M16 3c-2 0-3 2-3 4 2 0 3-2 3-4zm3 14c-1 2-2 4-4 4-1 0-2-1-3-1s-2 1-3 1c-2 0-5-5-5-9 0-4 4-6 7-4 1 1 2 1 3 0 2-1 4-1 6 1-4 2-4 6-1 8z"/></svg><span>Apple ID <small>Sắp có</small></span></button></div>
    <p class="auth-register">Chưa có tài khoản? <a href="${ctx}/register">Đăng ký ngay</a></p>
</section>
