package security;

import controller.CheckoutServlet;

import data.dao.CheckoutDao;

import filter.PendingCartFilter;

import jakarta.servlet.http.*;

import model.*;

import java.lang.reflect.Proxy;
import java.time.*;
import java.util.*;

public class PendingCartTest {

    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static RentalCart cart() {
        RentalCart c = new RentalCart();
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        c.add(1, 2, now, now.plusDays(3), "08:00", "17:00");
        return c;
    }

    static void add(RentalCart c, int id, int quantity) {
        c.add(id, quantity, c.getStart(), c.getEnd(), c.getPickupTime(), c.getReturnTime());
    }

    static class Checkout extends CheckoutServlet {

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }

        void get(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doGet(q, s);
        }
    }

    static void filter(RentalCart cart, int userId, String state) throws Exception {
        User user = AdminSecurityTest.user("user");
        user.setId(userId);
        HttpSession session = AdminSecurityTest.session(user, null);
        session.setAttribute("rentalCart", cart);
        HttpServletRequest original = AdminSecurityTest.request(session, Map.of());
        HttpServletRequest request =
                (HttpServletRequest)
                        Proxy.newProxyInstance(
                                PendingCartTest.class.getClassLoader(),
                                new Class<?>[] {HttpServletRequest.class},
                                (p, m, a) ->
                                        m.getName().equals("getServletPath")
                                                ? "/cart"
                                                : m.invoke(original, a));
        boolean[] queried = {false}, chained = {false};
        new PendingCartFilter() {
            protected CheckoutDao dao() {
                return new CheckoutDao() {
                    public String cartPaymentState(int id, int owner) {
                        queried[0] = true;
                        check(id == 10 && owner == 1, "Reconciliation uses owner and bound order");
                        return state;
                    }
                };
            }
        }.doFilter(
                request,
                AdminSecurityTest.response(new AdminSecurityTest.ResponseState()),
                (q, s) -> chained[0] = true);
        check(chained[0], "Request proceeds after reconciliation");
        if (userId != cart.getPendingUserId() && cart.getPendingOrderId() > 0) {
            check(!queried[0], "Other account cannot reconcile cart");
        }
    }

    public static void main(String[] args) throws Exception {
        RentalCart c = cart();
        c.beginPayment(10, 1);
        check(c.getCount() == 2, "Creating payment preserves cart");
        filter(c, 1, "pending");
        check(c.getCount() == 2 && c.getPendingOrderId() == 10, "Unpaid order preserves cart");
        filter(c, 1, "reported");
        check(
                c.getCount() == 2 && c.getPendingOrderId() == 10,
                "Reported transfer/receipt does not clear cart");
        add(c, 1, 1);
        add(c, 2, 1);
        filter(c, 1, "paid");
        check(
                c.getCount() == 2 && c.getQuantities().get(1) == 1 && c.getQuantities().get(2) == 1,
                "Paid order removes only original quantities");
        c.finishPayment(10, 1, true);
        check(c.getCount() == 2, "Repeated confirmation cannot remove more items");
        c = cart();
        c.beginPayment(10, 1);
        filter(c, 1, "cancelled");
        check(
                c.getCount() == 2 && c.getPendingOrderId() == 0,
                "Cancellation releases pending marker but keeps cart");
        c = cart();
        c.beginPayment(10, 1);
        filter(c, 2, "paid");
        check(c.getCount() == 2 && c.getPendingOrderId() == 10, "Other account cannot clear items");
        c = cart();
        c.beginPayment(10, 1);
        c.update(1, 1);
        filter(c, 1, "paid");
        check(c.getCount() == 0, "Reduced quantity reconciles safely");
        c = cart();
        LocalDate from = c.getStart(), to = c.getEnd();
        c.beginPayment(10, 1);
        c.remove(1);
        c.add(1, 1, from, to, "08:00", "17:00");
        filter(c, 1, "paid");
        check(c.getCount() == 1, "Removing then readding does not delete new items");
        c = cart();
        from = c.getStart();
        to = c.getEnd();
        c.beginPayment(10, 1);
        c.clear();
        c.add(1, 1, from.plusDays(1), to.plusDays(1), "08:00", "17:00");
        filter(c, 1, "paid");
        check(c.getCount() == 1, "Replacement cart/date remains after original payment");
        c = cart();
        c.beginPayment(10, 1);
        User user = AdminSecurityTest.user("user");
        user.setId(1);
        var session = AdminSecurityTest.session(user, null);
        session.setAttribute("shopCsrf", "token");
        session.setAttribute("rentalCart", c);
        var response = new AdminSecurityTest.ResponseState();
        new Checkout()
                .post(
                        AdminSecurityTest.request(session, Map.of("csrf", "token", "nonce", "old")),
                        AdminSecurityTest.response(response));
        check(
                response.location.endsWith("/checkout?order=10") && c.getCount() == 2,
                "Repeated checkout reuses pending order");
        response = new AdminSecurityTest.ResponseState();
        new Checkout()
                .get(
                        AdminSecurityTest.request(session, Map.of()),
                        AdminSecurityTest.response(response));
        check(
                response.location.endsWith("/checkout?order=10"),
                "Return to checkout resumes payment");
        System.out.println(
                "PASS: "
                        + checks
                        + " pending-cart lifecycle, ownership and duplicate-checkout checks; no"
                        + " database writes.");
    }
}
