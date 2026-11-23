package security;

import controller.MyRentalServlet;

import data.dao.MyRentalDao;

import jakarta.servlet.http.*;

import model.*;

import java.sql.SQLException;
import java.time.*;
import java.util.*;

public class CancelRentalTest {
    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    static class Endpoint extends MyRentalServlet {
        int calls, owner, order;
        boolean allowed = true, offline;

        protected MyRentalDao dao() {
            return new MyRentalDao() {
                public boolean cancelUnpaid(int id, int userId) throws SQLException {
                    calls++;
                    owner = userId;
                    order = id;
                    if (offline) throw new SQLException("fixture");
                    return allowed;
                }
            };
        }

        public void log(String message, Throwable cause) {}

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static RentalCart pending(int order, int userId) {
        RentalCart cart = new RentalCart();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        cart.add(1, 1, date, date.plusDays(1), "08:00", "17:00");
        cart.beginPayment(order, userId);
        return cart;
    }

    public static void main(String[] args) throws Exception {
        Endpoint endpoint = new Endpoint();
        var guest = AdminSecurityTest.session(null, null);
        var state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(guest, Map.of()), AdminSecurityTest.response(state));
        check(state.status == 401 && endpoint.calls == 0, "Guests cannot cancel");
        User user = new User(7, "Customer", "a@example.test", "0901234567", null, "user");
        var session = AdminSecurityTest.session(user, null);
        session.setAttribute("shopCsrf", "token");
        for (var parameters :
                List.of(
                        Map.of("csrf", "wrong", "action", "cancel", "order", "101"),
                        Map.of("csrf", "token", "action", "other", "order", "101"),
                        Map.of("csrf", "token", "action", "cancel", "order", "-1"))) {
            state = new AdminSecurityTest.ResponseState();
            endpoint.post(
                    AdminSecurityTest.request(session, parameters),
                    AdminSecurityTest.response(state));
            check(
                    state.status >= 400 && endpoint.calls == 0,
                    "Invalid action, CSRF and ID rejected before database");
        }
        RentalCart old = pending(42, 7), direct = pending(101, 7);
        session.setAttribute("rentalCart", old);
        session.setAttribute("directRentalCart", direct);
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(
                        session,
                        Map.of(
                                "csrf", "token", "action", "cancel", "order", "101", "user_id",
                                "99")),
                AdminSecurityTest.response(state));
        check(
                endpoint.owner == 7 && endpoint.order == 101,
                "Owner comes from session, not submitted ID");
        check(
                direct.getPendingOrderId() == 0 && direct.getCount() == 1,
                "Cancellation releases direct checkout marker without deleting selected items");
        check(
                old.getPendingOrderId() == 42 && old.getCount() == 1,
                "Unrelated original cart stays unchanged");
        check(
                state.status == 302
                        && state.location.endsWith("/my-rentals?order=101")
                        && session.getAttribute("rentalFlash") != null,
                "Success redirects to cancelled order with feedback");
        endpoint.allowed = false;
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "action", "cancel", "order", "42")),
                AdminSecurityTest.response(state));
        check(
                old.getPendingOrderId() == 42 && session.getAttribute("rentalFlashError") != null,
                "Rejected cancellation preserves pending order");
        endpoint.offline = true;
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "action", "cancel", "order", "42")),
                AdminSecurityTest.response(state));
        check(
                old.getPendingOrderId() == 42 && state.status == 302,
                "Database failure preserves cart and returns feedback");
        System.out.println(
                "PASS: "
                        + checks
                        + " customer cancellation access, ownership and cart checks; no database"
                        + " writes.");
    }
}
