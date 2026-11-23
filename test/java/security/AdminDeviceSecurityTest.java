package security;

import controller.AdminDeviceServlet;

import data.dao.AdminDeviceDao;

import jakarta.servlet.http.*;

import java.math.BigDecimal;
import java.util.*;

public class AdminDeviceSecurityTest {

    static int checks;

    static class Endpoint extends AdminDeviceServlet {

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static void reject(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid input rejection");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    public static void main(String[] args) throws Exception {
        reject(() -> AdminDeviceDao.validateSerial(""));
        reject(() -> AdminDeviceDao.validateSerial("\nSERIAL"));
        reject(() -> AdminDeviceDao.validateSerial("S".repeat(101)));
        reject(() -> AdminDeviceDao.validateStatus("renting"));
        reject(() -> AdminDeviceDao.validateStatus(null));
        reject(() -> AdminDeviceDao.validatePrice(new BigDecimal("-1")));
        reject(() -> AdminDeviceDao.validatePrice(new BigDecimal("10000000000")));
        reject(() -> AdminDeviceDao.validatePrice(new BigDecimal("10.123")));
        AdminDeviceDao.validateSerial("CAM-0001");
        AdminDeviceDao.validateStatus("maintenance");
        AdminDeviceDao.validatePrice(new BigDecimal("350000.00"));
        checks++;
        var session = AdminSecurityTest.session(AdminSecurityTest.user("admin"), "expected-token");
        for (var params : List.of(Map.<String, String>of(), Map.of("csrf", "wrong"))) {
            var state = new AdminSecurityTest.ResponseState();
            new Endpoint()
                    .post(
                            AdminSecurityTest.request(session, params),
                            AdminSecurityTest.response(state));
            if (state.status != 403) {
                throw new AssertionError("CSRF must reject");
            }
            checks++;
        }
        var state = new AdminSecurityTest.ResponseState();
        new Endpoint()
                .post(
                        AdminSecurityTest.request(
                                session, Map.of("csrf", "expected-token", "action", "status")),
                        AdminSecurityTest.response(state));
        if (state.status != 302 || session.getAttribute("deviceFlashError") == null) {
            throw new AssertionError("Missing selected IDs must reject");
        }
        checks++;
        System.out.println(
                "PASS: " + checks + " device validation/CSRF checks; no database writes.");
    }
}
