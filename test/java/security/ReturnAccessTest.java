package security;

import controller.AdminRentalServlet;

import data.dao.RentalReturnDao;

import jakarta.servlet.http.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ReturnAccessTest {

    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static class Endpoint extends AdminRentalServlet {

        int received, confirmed, actor, id;
        BigDecimal amount;
        boolean paid;

        protected RentalReturnDao returnDao() {
            return new RentalReturnDao() {
                public void receive(
                        int order,
                        int user,
                        LocalDateTime actual,
                        String legacy,
                        BigDecimal fee,
                        String reason,
                        boolean collected) {
                    received++;
                    id = order;
                    actor = user;
                    amount = fee;
                    paid = collected;
                }

                public boolean confirmFee(int order, int user) {
                    confirmed++;
                    id = order;
                    actor = user;
                    return true;
                }
            };
        }

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    public static void main(String[] args) throws Exception {
        Endpoint e = new Endpoint();
        var admin = AdminSecurityTest.user("admin");
        admin.setId(1);
        var session = AdminSecurityTest.session(admin, "token");
        var state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "wrong", "action", "return_rental")),
                AdminSecurityTest.response(state));
        check(state.status == 403 && e.received == 0, "Return action requires CSRF");
        var customer = AdminSecurityTest.session(AdminSecurityTest.user("user"), "token");
        state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        customer, Map.of("csrf", "token", "action", "confirm_late_fee", "id", "1")),
                AdminSecurityTest.response(state));
        check(state.status == 403 && e.confirmed == 0, "Customer cannot confirm late fee");
        state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        session,
                        Map.of(
                                "csrf",
                                "token",
                                "action",
                                "return_rental",
                                "id",
                                "5",
                                "actual_return",
                                "invalid")),
                AdminSecurityTest.response(state));
        check(state.status == 302 && e.received == 0, "Invalid actual date does not reach DAO");
        state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        session,
                        Map.of(
                                "csrf",
                                "token",
                                "action",
                                "return_rental",
                                "id",
                                "5",
                                "actual_return",
                                "2026-01-02T20:00",
                                "legacy_due_time",
                                "17:00",
                                "actor",
                                "999",
                                "adjusted_fee",
                                "0",
                                "adjustment_reason",
                                "Khách báo trước",
                                "fee_collected",
                                "1")),
                AdminSecurityTest.response(state));
        check(
                e.received == 1 && e.id == 5 && e.actor == 1 && BigDecimal.ZERO.equals(e.amount),
                "Return actor comes from session, not submitted actor");
        check(
                e.paid && state.location.endsWith("/admin/rentals?order=5"),
                "Collection intent preserved and returned order stays selected");
        state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        session, Map.of("csrf", "token", "action", "confirm_late_fee", "id", "5")),
                AdminSecurityTest.response(state));
        check(
                e.confirmed == 1 && e.actor == 1,
                "Fee confirmation uses authenticated administrator");
        state = new AdminSecurityTest.ResponseState();
        e.post(
                AdminSecurityTest.request(
                        session,
                        Map.of(
                                "csrf",
                                "token",
                                "id",
                                "5",
                                "expected",
                                "renting",
                                "status",
                                "returned")),
                AdminSecurityTest.response(state));
        check(
                e.received == 1 && session.getAttribute("adminFlashError") != null,
                "Legacy status request cannot skip return assessment");
        System.out.println(
                "PASS: "
                        + checks
                        + " return/fee HTTP authorization and request-routing checks; no database"
                        + " writes.");
    }
}
