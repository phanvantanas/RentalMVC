package security;

import data.dao.CheckoutDao;

import filter.RentalHoldFilter;

import jakarta.servlet.http.*;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;

public class RentalHoldFilterTest {
    static int checks;

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    static void access(String path, boolean expected, boolean fail) throws Exception {
        List<String> calls = new ArrayList<>();
        var base = AdminSecurityTest.request(AdminSecurityTest.session(null, null), Map.of());
        HttpServletRequest request =
                (HttpServletRequest)
                        Proxy.newProxyInstance(
                                RentalHoldFilterTest.class.getClassLoader(),
                                new Class<?>[] {HttpServletRequest.class},
                                (p, m, a) ->
                                        m.getName().equals("getServletPath")
                                                ? path
                                                : m.invoke(base, a));
        var state = new AdminSecurityTest.ResponseState();
        new RentalHoldFilter() {
            protected CheckoutDao dao() {
                return new CheckoutDao() {
                    public int expirePending() throws SQLException {
                        calls.add("expire");
                        if (fail) throw new SQLException("fixture");
                        return 1;
                    }
                };
            }
        }.doFilter(request, AdminSecurityTest.response(state), (q, s) -> calls.add("page"));
        check(
                calls.equals(
                        fail
                                ? List.of("expire")
                                : expected ? List.of("expire", "page") : List.of("page")),
                "Expiry precedes page processing: " + path);
        check(
                state.status == (fail ? 503 : 200),
                "Database failure prevents stale inventory: " + path);
    }

    public static void main(String[] args) throws Exception {
        for (String path :
                List.of(
                        "",
                        "/",
                        "/home",
                        "/cart",
                        "/checkout",
                        "/my-rentals",
                        "/payment/receipt",
                        "/admin",
                        "/admin/rentals")) access(path, true, false);
        access("/assets/js/shop.js", false, false);
        access("/login", false, false);
        access("/checkout", true, true);
        System.out.println("PASS: " + checks + " rental hold filter checks; no database writes.");
    }
}
