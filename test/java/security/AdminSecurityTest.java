package security;

import controller.AdminRentalServlet;

import data.dao.AdminRentalDao;

import filter.AdminAccessFilter;

import jakarta.servlet.http.*;

import model.User;

import java.lang.reflect.Proxy;
import java.util.*;

/** Run with the compiled application and servlet-api.jar on the classpath. No database writes. */
public class AdminSecurityTest {
    static int checks;

    static class ResponseState {
        int status = 200;
        String location;
    }

    static class PostEndpoint extends AdminRentalServlet {
        void post(HttpServletRequest req, HttpServletResponse res) throws Exception {
            super.doPost(req, res);
        }
    }

    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        return 0;
    }

    static HttpSession session(Object user, String csrf) {
        Map<String, Object> attrs = new HashMap<>();
        if (user != null) attrs.put("user", user);
        if (csrf != null) attrs.put("adminCsrf", csrf);
        return (HttpSession)
                Proxy.newProxyInstance(
                        AdminSecurityTest.class.getClassLoader(),
                        new Class<?>[] {HttpSession.class},
                        (p, m, a) -> {
                            if (m.getName().equals("getAttribute")) return attrs.get(a[0]);
                            if (m.getName().equals("setAttribute")) {
                                attrs.put((String) a[0], a[1]);
                                return null;
                            }
                            return defaultValue(m.getReturnType());
                        });
    }

    static HttpServletRequest request(HttpSession session, Map<String, String> params) {
        return (HttpServletRequest)
                Proxy.newProxyInstance(
                        AdminSecurityTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletRequest.class},
                        (p, m, a) -> {
                            switch (m.getName()) {
                                case "getSession":
                                    return session;
                                case "getContextPath":
                                    return "/RentalCamera";
                                case "getParameter":
                                    return params.get(a[0]);
                                default:
                                    return defaultValue(m.getReturnType());
                            }
                        });
    }

    static HttpServletResponse response(ResponseState state) {
        return (HttpServletResponse)
                Proxy.newProxyInstance(
                        AdminSecurityTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletResponse.class},
                        (p, m, a) -> {
                            if (m.getName().equals("sendError") || m.getName().equals("setStatus"))
                                state.status = (Integer) a[0];
                            if (m.getName().equals("sendRedirect")) {
                                state.status = 302;
                                state.location = (String) a[0];
                            }
                            return defaultValue(m.getReturnType());
                        });
    }

    static User user(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    static void access(Object user, boolean hasSession, int status, boolean allowed)
            throws Exception {
        ResponseState state = new ResponseState();
        boolean[] chained = {false};
        new AdminAccessFilter()
                .doFilter(
                        request(hasSession ? session(user, null) : null, Map.of()),
                        response(state),
                        (q, s) -> chained[0] = true);
        check(state.status == status && chained[0] == allowed, "Access control: " + user);
        if (!hasSession) check("/RentalCamera/login".equals(state.location), "Guest redirect");
    }

    public static void main(String[] args) throws Exception {
        access(null, false, 302, false);
        access(user("customer"), true, 403, false);
        access(user("admin"), true, 200, true);
        access(user("ADMIN"), true, 200, true);
        access(user(null), true, 403, false);
        access("admin", true, 302, false);
        for (Map<String, String> params :
                List.of(Map.<String, String>of(), Map.of("csrf", "wrong"))) {
            ResponseState state = new ResponseState();
            new PostEndpoint()
                    .post(
                            request(session(user("admin"), "expected-token"), params),
                            response(state));
            check(state.status == 403, "CSRF rejection");
        }
        ResponseState state = new ResponseState();
        new PostEndpoint()
                .post(
                        request(
                                session(user("admin"), "expected-token"),
                                Map.of("csrf", "expected-token")),
                        response(state));
        check(state.status == 400, "Missing action rejection");
        state = new ResponseState();
        new PostEndpoint()
                .post(
                        request(
                                session(user("admin"), "expected-token"),
                                Map.of(
                                        "csrf",
                                        "expected-token",
                                        "expected",
                                        "pending",
                                        "status",
                                        "confirmed",
                                        "id",
                                        "bad")),
                        response(state));
        check(state.status == 400, "Invalid ID rejection");
        check(
                new AdminRentalDao().transition(1, "returned", "confirmed") != null,
                "Reject invalid transition before database access");
        System.out.println("PASS: " + checks + " admin security checks; no database writes.");
    }
}
