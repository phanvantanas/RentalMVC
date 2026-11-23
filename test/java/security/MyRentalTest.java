package security;

import controller.MyRentalServlet;

import data.dao.MyRentalDao;
import data.driver.MySQLDriver;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import model.User;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class MyRentalTest {
    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    static class Endpoint extends MyRentalServlet {
        List<Map<String, Object>> result = List.of();
        int owner;
        boolean unavailable;

        protected MyRentalDao dao() {
            return new MyRentalDao() {
                public List<Map<String, Object>> findOwned(int userId, Integer id)
                        throws SQLException {
                    owner = userId;
                    if (unavailable) throw new SQLException("Test offline");
                    return result;
                }
            };
        }

        public void log(String message, Throwable cause) {}

        void get(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doGet(q, s);
        }
    }

    static HttpServletRequest request(
            HttpSession session, Map<String, String> params, Map<String, Object> attrs) {
        var original = AdminSecurityTest.request(session, params);
        return (HttpServletRequest)
                Proxy.newProxyInstance(
                        MyRentalTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletRequest.class},
                        (p, m, a) -> {
                            if (m.getName().equals("setAttribute")) {
                                attrs.put((String) a[0], a[1]);
                                return null;
                            }
                            if (m.getName().equals("getRequestDispatcher"))
                                return new RequestDispatcher() {
                                    public void forward(ServletRequest q, ServletResponse s) {
                                        attrs.put("forward", a[0]);
                                    }

                                    public void include(ServletRequest q, ServletResponse s) {}
                                };
                            return m.invoke(original, a);
                        });
    }

    public static void main(String[] args) throws Exception {
        Endpoint endpoint = new Endpoint();
        var attrs = new HashMap<String, Object>();
        var guest = AdminSecurityTest.session(null, null);
        var response = new AdminSecurityTest.ResponseState();
        endpoint.get(
                request(guest, Map.of("order", "5"), attrs), AdminSecurityTest.response(response));
        check(response.location.endsWith("/login"), "Guests must sign in");
        check(
                "/my-rentals?order=5".equals(guest.getAttribute("afterLogin")),
                "Preserve safe tracking destination");
        User user = AdminSecurityTest.user("user");
        user.setId(1);
        var session = AdminSecurityTest.session(user, null);
        response = new AdminSecurityTest.ResponseState();
        endpoint.get(
                request(session, Map.of("order", "-1"), attrs),
                AdminSecurityTest.response(response));
        check(response.status == 400, "Invalid IDs rejected");
        response = new AdminSecurityTest.ResponseState();
        endpoint.get(
                request(session, Map.of("order", "2", "user_id", "2"), attrs),
                AdminSecurityTest.response(response));
        check(
                response.status == 404 && endpoint.owner == 1,
                "Order owner always comes from session; missing or foreign order is 404");
        endpoint.result = List.of(Map.of("id", 1));
        attrs.clear();
        response = new AdminSecurityTest.ResponseState();
        endpoint.get(request(session, Map.of(), attrs), AdminSecurityTest.response(response));
        check(
                endpoint.owner == 1 && attrs.get("myOrders") == endpoint.result,
                "Only owned data is passed to page");
        check(
                "/WEB-INF/views/shop/my-rentals.jsp".equals(attrs.get("forward")),
                "Protected JSP view");
        endpoint.unavailable = true;
        attrs.clear();
        response = new AdminSecurityTest.ResponseState();
        endpoint.get(request(session, Map.of(), attrs), AdminSecurityTest.response(response));
        check(
                response.status == 503 && attrs.get("loadError") != null,
                "Database error is not an empty history");
        try (Connection con = MySQLDriver.getConnection()) {
            try (Statement s = con.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE rentals(id INT,user_id INT,status"
                            + " VARCHAR(20),start_date DATE,end_date DATE,total"
                            + " DECIMAL(12,2),late_fee DECIMAL(12,2),created_at TIMESTAMP)");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_payments(rental_id INT,status"
                            + " VARCHAR(20),pickup_time TIME,return_time TIME,transfer_reference"
                            + " VARCHAR(40))");
                s.execute("CREATE TEMPORARY TABLE rental_receipts(rental_id INT)");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_returns(rental_id INT,actual_return_at"
                            + " DATETIME,fee_status VARCHAR(20),calculated_fee"
                            + " DECIMAL(12,2),adjustment_reason VARCHAR(500))");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_items(id INT,rental_id INT,device_id"
                            + " INT,price_per_day DECIMAL(12,2))");
                s.execute(
                        "CREATE TEMPORARY TABLE devices(id INT,product_id INT,serial"
                            + " VARCHAR(100))");
                s.execute("CREATE TEMPORARY TABLE products(id INT,name VARCHAR(150))");
                s.execute(
                        "INSERT INTO rentals"
                            + " VALUES(1,1,'returned','2026-01-01','2026-01-02',100,5,'2026-01-01'),(2,2,'pending','2026-01-01','2026-01-02',200,0,'2026-01-01'),(3,1,'cancelled','2026-01-01','2026-01-02',300,0,'2026-01-01')");
                s.execute("INSERT INTO products VALUES(1,'Camera'),(2,'Lens')");
                s.execute("INSERT INTO devices VALUES(1,1,'SN-1'),(2,2,'SN-2')");
                s.execute("INSERT INTO rental_items VALUES(1,1,1,60),(2,1,2,40),(3,2,1,200)");
                s.execute(
                        "INSERT INTO rental_payments"
                            + " VALUES(1,'paid','08:00','17:00','TEST1'),(2,'reported','09:00','18:00','TEST2')");
                s.execute("INSERT INTO rental_receipts VALUES(1),(2)");
                s.execute(
                        "INSERT INTO rental_returns VALUES(1,'2026-01-02 20:00','unpaid',10,'Khách"
                            + " báo trước')");
            }
            MyRentalDao dao =
                    new MyRentalDao() {
                        protected Connection connection() {
                            return (Connection)
                                    Proxy.newProxyInstance(
                                            getClass().getClassLoader(),
                                            new Class<?>[] {Connection.class},
                                            (p, m, a) -> {
                                                if (m.getName().equals("close")) return null;
                                                try {
                                                    return m.invoke(con, a);
                                                } catch (InvocationTargetException e) {
                                                    throw e.getCause();
                                                }
                                            });
                        }
                    };
            var rows = dao.findOwned(1, null);
            check(rows.size() == 2, "One row per owned order, including cancelled history");
            var order = dao.findOwned(1, 1).get(0);
            check(
                    ((List<?>) order.get("items")).size() == 2,
                    "All equipment lines grouped into one order");
            check(
                    "paid".equals(order.get("paymentStatus"))
                            && Boolean.TRUE.equals(order.get("receiptAvailable")),
                    "Payment and receipt status available to owner");
            check(
                    order.get("actualReturnAt") != null
                            && "unpaid".equals(order.get("lateFeeStatus")),
                    "Owned return and outstanding fee visible");
            check(
                    "Khách báo trước".equals(order.get("feeAdjustmentReason")),
                    "Owned fee adjustment explanation visible");
            check(
                    "08:00".equals(order.get("pickupTime"))
                            && "17:00".equals(order.get("returnTime")),
                    "Pickup and return times");
            check(dao.findOwned(1, 2).isEmpty(), "Foreign order cannot be loaded by guessed ID");
            check(dao.findOwned(2, 1).isEmpty(), "Ownership guard works in both directions");
            var old = dao.findOwned(1, 3).get(0);
            check(
                    Boolean.FALSE.equals(old.get("hasPayment"))
                            && ((List<?>) old.get("items")).isEmpty(),
                    "Legacy order supports missing payment and equipment data");
            check(
                    ((Number) old.get("step")).intValue() == 0,
                    "Cancelled order has no success progress");
            check(dao.findOwned(99, null).isEmpty(), "Unrelated customer sees no orders");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " customer tracking/authentication/ownership checks using TEMPORARY"
                        + " tables; original data unchanged.");
    }
}
