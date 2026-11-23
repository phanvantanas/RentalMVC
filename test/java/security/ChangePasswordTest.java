package security;

import controller.ProfileServlet;

import data.dao.ProfileDao;

import jakarta.servlet.http.*;

import model.User;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;

public class ChangePasswordTest {

    static int checks, rotations;

    static void check(boolean ok, String label) {
        if (!ok) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static class Endpoint extends ProfileServlet {

        int calls, owner;
        String current, next;
        boolean wrong, offline;

        protected ProfileDao dao() {
            return new ProfileDao() {
                public void changePassword(int id, String old, String value, String confirm)
                        throws SQLException {
                    calls++;
                    owner = id;
                    current = old;
                    next = value;
                    if (wrong) {
                        throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
                    }
                    if (offline) {
                        throw new SQLException("fixture");
                    }
                }
            };
        }

        public void log(String message, Throwable cause) {}

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static HttpServletRequest request(
            HttpSession session, Map<String, String> params, Map<String, Object> attrs) {
        var base = MyRentalTest.request(session, params, attrs);
        return (HttpServletRequest)
                Proxy.newProxyInstance(
                        ChangePasswordTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletRequest.class},
                        (p, m, a) -> {
                            if (m.getName().equals("changeSessionId")) {
                                rotations++;
                                return "rotated";
                            }
                            if (m.getName().equals("getAttribute")) {
                                return attrs.get(a[0]);
                            }
                            return m.invoke(base, a);
                        });
    }

    static Map<String, String> fields(String csrf, String current, String next, String confirm) {
        return Map.of(
                "action",
                "change_password",
                "csrf",
                csrf,
                "current_password",
                current,
                "new_password",
                next,
                "confirm_password",
                confirm,
                "id",
                "999");
    }

    public static void main(String[] args) throws Exception {
        Endpoint endpoint = new Endpoint();
        var attrs = new HashMap<String, Object>();
        var state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                request(
                        AdminSecurityTest.session(null, null),
                        fields("token", "old-password", "new-password", "new-password"),
                        attrs),
                AdminSecurityTest.response(state));
        check(state.status == 401 && endpoint.calls == 0, "Guest cannot change password");
        User user = new User(7, "Customer", "a@example.test", "0901234567", null, "user");
        var session = AdminSecurityTest.session(user, null);
        session.setAttribute("shopCsrf", "token");
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                request(
                        session,
                        fields("wrong", "old-password", "new-password", "new-password"),
                        attrs),
                AdminSecurityTest.response(state));
        check(state.status == 403 && endpoint.calls == 0, "CSRF rejected before password lookup");
        for (var values :
                List.of(
                        new String[] {"", "new-password", "new-password"},
                        new String[] {"old-password", "short", "short"},
                        new String[] {"old-password", "new-password", "different"},
                        new String[] {"old-password", "old-password", "old-password"},
                        new String[] {"old-password", "x".repeat(129), "x".repeat(129)})) {
            attrs.clear();
            state = new AdminSecurityTest.ResponseState();
            endpoint.post(
                    request(session, fields("token", values[0], values[1], values[2]), attrs),
                    AdminSecurityTest.response(state));
            check(
                    state.status == 400
                            && endpoint.calls == 0
                            && attrs.get("passwordError") != null,
                    "Invalid password fields rejected before DAO");
        }
        endpoint.wrong = true;
        attrs.clear();
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                request(
                        session,
                        fields("token", "wrong-password", "  new-password  ", "  new-password  "),
                        attrs),
                AdminSecurityTest.response(state));
        check(
                state.status == 400 && rotations == 0 && attrs.get("passwordError") != null,
                "Wrong current password returns error without rotating session");
        check(
                !attrs.containsKey("current_password")
                        && !attrs.containsKey("new_password")
                        && !attrs.containsKey("confirm_password"),
                "Passwords never stored as page draft");
        endpoint.wrong = false;
        endpoint.offline = true;
        attrs.clear();
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                request(
                        session,
                        fields("token", "old-password", "new-password", "new-password"),
                        attrs),
                AdminSecurityTest.response(state));
        check(state.status == 503 && rotations == 0, "Database failure does not report success");
        endpoint.offline = false;
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                request(
                        session,
                        fields("token", "old-password", "  new-password  ", "  new-password  "),
                        attrs),
                AdminSecurityTest.response(state));
        check(
                endpoint.owner == 7 && "  new-password  ".equals(endpoint.next),
                "Authenticated owner used and password whitespace preserved");
        check(
                rotations == 1
                        && state.status == 302
                        && state.location.endsWith("/profile")
                        && session.getAttribute("passwordSuccess") != null,
                "Success rotates session ID and redirects with feedback");
        check(
                user.getPassword() == null
                        && session.getAttribute("new_password") == null
                        && session.getAttribute("current_password") == null,
                "Passwords never copied to session");
        System.out.println(
                "PASS: "
                        + checks
                        + " password change authentication, CSRF, validation and session checks; no"
                        + " database writes.");
    }
}
