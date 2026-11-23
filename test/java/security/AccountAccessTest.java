package security;

import controller.AdminUserServlet;

import data.dao.AdminUserDao;

import filter.AccountAccessFilter;

import jakarta.servlet.http.*;

import model.User;

import java.lang.reflect.*;
import java.util.*;

public class AccountAccessTest {

    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static class Endpoint extends AdminUserServlet {

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static void access(
            String path, String method, User old, User fresh, int expected, boolean allowed)
            throws Exception {
        var session = AdminSecurityTest.session(old, null);
        boolean[] invalidated = {false}, chained = {false}, queried = {false};
        HttpSession wrapped =
                (HttpSession)
                        Proxy.newProxyInstance(
                                AccountAccessTest.class.getClassLoader(),
                                new Class<?>[] {HttpSession.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("invalidate")) {
                                        invalidated[0] = true;
                                        return null;
                                    }
                                    return m.invoke(session, a);
                                });
        var base = AdminSecurityTest.request(wrapped, Map.of());
        HttpServletRequest q =
                (HttpServletRequest)
                        Proxy.newProxyInstance(
                                AccountAccessTest.class.getClassLoader(),
                                new Class<?>[] {HttpServletRequest.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("getServletPath")) {
                                        return path;
                                    }
                                    if (m.getName().equals("getMethod")) {
                                        return method;
                                    }
                                    return m.invoke(base, a);
                                });
        var state = new AdminSecurityTest.ResponseState();
        new AccountAccessFilter() {
            protected AdminUserDao dao() {
                return new AdminUserDao() {
                    public User activePrincipal(int id) {
                        queried[0] = true;
                        return fresh;
                    }
                };
            }
        }.doFilter(q, AdminSecurityTest.response(state), (req, res) -> chained[0] = true);
        check(
                state.status == expected && chained[0] == allowed,
                "Account filter: " + path + " " + method);
        if (old != null
                && fresh == null
                && !path.startsWith("/assets/")
                && !path.equals("/logout")) {
            check(invalidated[0], "Locked/deleted account invalidates old session");
        }
        if (path.startsWith("/assets/") || path.equals("/logout")) {
            check(!queried[0], "Static assets and logout do not require database");
        }
        if (allowed && fresh != null && old != null) {
            check(session.getAttribute("user") == fresh, "Session role/profile refreshed");
        }
    }

    public static void main(String[] args) throws Exception {
        User admin = AdminSecurityTest.user("admin"), customer = AdminSecurityTest.user("user");
        admin.setId(1);
        admin.setName("Admin");
        customer.setId(1);
        customer.setName("Customer");
        access("/admin/users", "GET", admin, customer, 403, false);
        access("/checkout", "POST", customer, null, 401, false);
        access("/my-rentals", "GET", customer, null, 302, false);
        access("/admin/users", "GET", admin, admin, 200, true);
        access("/home", "GET", admin, customer, 200, true);
        access("/assets/css/admin.css", "GET", admin, null, 200, true);
        access("/logout", "GET", customer, null, 200, true);
        access("/home", "GET", null, null, 200, true);
        for (User principal : Arrays.asList(null, customer, admin)) {
            var session = AdminSecurityTest.session(principal, "expected");
            var state = new AdminSecurityTest.ResponseState();
            new Endpoint()
                    .post(
                            AdminSecurityTest.request(session, Map.of("csrf", "wrong")),
                            AdminSecurityTest.response(state));
            check(
                    state.status == (principal == null ? 401 : principal == customer ? 403 : 403),
                    "Admin user mutation requires authentication, role and CSRF");
        }
        var session = AdminSecurityTest.session(admin, "expected");
        var state = new AdminSecurityTest.ResponseState();
        new Endpoint()
                .post(
                        AdminSecurityTest.request(
                                session,
                                Map.of(
                                        "csrf",
                                        "expected",
                                        "action",
                                        "create",
                                        "name",
                                        "Draft Name",
                                        "email",
                                        "draft@example.test",
                                        "password",
                                        "Fixture One",
                                        "confirm",
                                        "Fixture Two")),
                        AdminSecurityTest.response(state));
        check(
                state.status == 302 && session.getAttribute("userError") != null,
                "Password confirmation rejected before DAO");
        check(
                !((Map<?, ?>) session.getAttribute("userDraft")).containsKey("password"),
                "Draft never retains passwords");
        System.out.println(
                "PASS: " + checks + " account session/role/CSRF checks; no database writes.");
    }
}
