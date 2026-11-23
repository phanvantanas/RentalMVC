package security;

import controller.*;

import data.dao.CheckoutDao;

import filter.PendingCartFilter;

import jakarta.servlet.http.*;

import model.*;

import service.MerchantBank;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class DirectCheckoutTest {

    static int checks;

    static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
        checks++;
    }

    static class Cart extends CartServlet {

        protected Product product(int id) {
            return new Product(id, 1, "Canon EOS M10", "", 150000, 0, "", "active");
        }

        protected int available(int id, LocalDate start, LocalDate end) {
            return 1;
        }

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static class Checkout extends CheckoutServlet {

        int places;
        RentalCart placed;
        BigDecimal amount;

        protected MerchantBank bank() {
            return new MerchantBank("BIDV", "0000000000", "TEST FIXTURE");
        }

        protected CheckoutDao dao() {
            return new CheckoutDao() {
                public int place(int id, RentalCart cart, MerchantBank bank, BigDecimal quote) {
                    places++;
                    placed = cart;
                    amount = quote;
                    return 101;
                }
            };
        }

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }

        void get(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doGet(q, s);
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        HttpSession session =
                (HttpSession)
                        Proxy.newProxyInstance(
                                DirectCheckoutTest.class.getClassLoader(),
                                new Class<?>[] {HttpSession.class},
                                (p, m, a) -> {
                                    switch (m.getName()) {
                                        case "getAttribute":
                                            return attrs.get(a[0]);
                                        case "setAttribute":
                                            attrs.put((String) a[0], a[1]);
                                            return null;
                                        case "removeAttribute":
                                            attrs.remove(a[0]);
                                            return null;
                                        default:
                                            return AdminSecurityTest.defaultValue(
                                                    m.getReturnType());
                                    }
                                });
        User user = new User(1, "Customer", "a@example.test", "0901234567", null, "user");
        attrs.put("user", user);
        attrs.put("shopCsrf", "token");
        LocalDate start = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        RentalCart old = new RentalCart();
        old.add(99, 1, start, start.plusDays(3), "08:00", "17:00");
        old.beginPayment(42, 1);
        attrs.put("rentalCart", old);
        Map<String, String> buy =
                Map.of(
                        "csrf",
                        "token",
                        "action",
                        "buy",
                        "product",
                        "1",
                        "start",
                        start.toString(),
                        "end",
                        start.plusDays(1).toString(),
                        "pickup_time",
                        "12:00",
                        "return_time",
                        "16:00");
        var state = new AdminSecurityTest.ResponseState();
        new Cart().post(AdminSecurityTest.request(session, buy), AdminSecurityTest.response(state));
        check(
                "/RentalCamera/checkout?direct=1".equals(state.location),
                "Buy opens selected device checkout instead of old order 42");
        RentalCart direct = (RentalCart) attrs.get("directRentalCart");
        check(
                direct != old
                        && direct.getCount() == 1
                        && direct.getQuantities().containsKey(1)
                        && direct.getDays() == 1,
                "Direct checkout contains one newly selected machine for one day");
        check(
                "12:00".equals(direct.getPickupTime()) && "16:00".equals(direct.getReturnTime()),
                "Selected times retained");
        check(
                old.getPendingOrderId() == 42
                        && old.getDays() == 3
                        && old.getQuantities().containsKey(99),
                "Old cart and pending payment preserved");
        attrs.put("checkoutNonce", "old-cart");
        attrs.put("checkoutQuote", new BigDecimal("675000"));
        attrs.put("checkoutNonceDirect", "new-device");
        attrs.put("checkoutQuoteDirect", new BigDecimal("150000"));
        Checkout checkout = new Checkout();
        state = new AdminSecurityTest.ResponseState();
        checkout.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "direct", "1", "nonce", "old-cart")),
                AdminSecurityTest.response(state));
        check(
                state.status == 409 && checkout.places == 0,
                "Old cart token cannot submit direct checkout");
        state = new AdminSecurityTest.ResponseState();
        checkout.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "direct", "1", "nonce", "new-device")),
                AdminSecurityTest.response(state));
        check(
                checkout.places == 1
                        && checkout.placed == direct
                        && checkout.amount.compareTo(new BigDecimal("150000")) == 0,
                "New order uses selected machine and 150000 quote instead of old 675000");
        check(
                "/RentalCamera/checkout?order=101".equals(state.location)
                        && direct.getPendingOrderId() == 101,
                "QR belongs to new order 101");
        check(
                attrs.get("checkoutQuote").equals(new BigDecimal("675000"))
                        && old.getPendingOrderId() == 42,
                "Direct order never changes existing cart payment");
        state = new AdminSecurityTest.ResponseState();
        checkout.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "direct", "1", "nonce", "new-device")),
                AdminSecurityTest.response(state));
        check(
                checkout.places == 1 && state.location.endsWith("order=101"),
                "Repeat submit reuses only the matching direct order");
        state = new AdminSecurityTest.ResponseState();
        new Cart().post(AdminSecurityTest.request(session, buy), AdminSecurityTest.response(state));
        check(
                state.location.endsWith("/cart") && attrs.get("cartError") != null,
                "Another buy reports unfinished direct order instead of opening its old QR");
        var base = AdminSecurityTest.request(session, Map.of());
        HttpServletRequest request =
                (HttpServletRequest)
                        Proxy.newProxyInstance(
                                DirectCheckoutTest.class.getClassLoader(),
                                new Class<?>[] {HttpServletRequest.class},
                                (p, m, a) ->
                                        m.getName().equals("getServletPath")
                                                ? "/checkout"
                                                : m.invoke(base, a));
        new PendingCartFilter() {
            protected CheckoutDao dao() {
                return new CheckoutDao() {
                    public String cartPaymentState(int id, int owner) {
                        return id == 101 ? "paid" : "pending";
                    }
                };
            }
        }.doFilter(
                request,
                AdminSecurityTest.response(new AdminSecurityTest.ResponseState()),
                (q, s) -> {});
        check(
                direct.getCount() == 0
                        && direct.getPendingOrderId() == 0
                        && old.getPendingOrderId() == 42
                        && old.getCount() == 1,
                "Paid direct order reconciles separately from original cart");
        attrs.remove("user");
        state = new AdminSecurityTest.ResponseState();
        checkout.get(
                AdminSecurityTest.request(session, Map.of("direct", "1")),
                AdminSecurityTest.response(state));
        check(
                "/checkout?direct=1".equals(attrs.get("afterLogin")),
                "Guest retains selected checkout through login");
        System.out.println(
                "PASS: "
                        + checks
                        + " direct checkout, stale QR, amount and cart isolation checks; no"
                        + " database writes.");
    }
}
