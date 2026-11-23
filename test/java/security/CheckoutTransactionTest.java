package security;

import data.dao.CheckoutDao;
import data.driver.MySQLDriver;

import model.RentalCart;

import service.MerchantBank;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;

public class CheckoutTransactionTest {

    static int checks;

    static void check(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static class Dao extends CheckoutDao {

        final Connection con;
        int opens, prepared;

        Dao(Connection con) {
            this.con = con;
        }

        protected Connection connection() {
            opens++;
            return (Connection)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {Connection.class},
                            (p, m, a) -> {
                                if (m.getName().equals("close")) {
                                    if (!con.getAutoCommit()) {
                                        con.setAutoCommit(true);
                                    }
                                    return null;
                                }
                                if (m.getName().equals("prepareStatement")) {
                                    prepared++;
                                }
                                try {
                                    return m.invoke(con, a);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            });
        }
    }

    static RentalCart cart(int product, int qty, int days) {
        RentalCart cart = new RentalCart();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        cart.add(product, qty, date, date.plusDays(days), "08:00", "17:00");
        return cart;
    }

    static int scalar(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement();
                var r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = MySQLDriver.getConnection()) {
            if (con == null) {
                throw new AssertionError("Database unavailable");
            }
            String[] tables = {
                "products(id INT PRIMARY KEY,id_category INT,name VARCHAR(150),image"
                    + " VARCHAR(255),price DECIMAL(12,2),description TEXT,status VARCHAR(20))",
                "devices(id INT PRIMARY KEY,product_id INT,serial VARCHAR(100),status VARCHAR(20))",
                "rentals(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,start_date DATE,end_date"
                    + " DATE,status VARCHAR(20),total DECIMAL(12,2),late_fee DECIMAL(12,2))",
                "rental_items(id INT AUTO_INCREMENT PRIMARY KEY,rental_id INT,device_id"
                    + " INT,price_per_day DECIMAL(12,2))",
                "rental_receipts(rental_id INT PRIMARY KEY,content_type VARCHAR(30),image_data"
                    + " MEDIUMBLOB,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                "rental_payments(rental_id INT PRIMARY KEY,amount DECIMAL(12,2),transfer_reference"
                    + " VARCHAR(40) UNIQUE,bank_code VARCHAR(20),account_number"
                    + " VARCHAR(40),account_holder VARCHAR(120),pickup_time TIME,return_time"
                    + " TIME,status VARCHAR(20) DEFAULT 'pending',created_at TIMESTAMP DEFAULT"
                    + " CURRENT_TIMESTAMP)"
            };
            for (String definition : tables)
                try (var statement = con.createStatement()) {
                    statement.execute("CREATE TEMPORARY TABLE " + definition);
                }
            try (var s = con.createStatement()) {
                s.executeUpdate(
                        "INSERT INTO products(id,id_category,name,image,price,description,status)"
                            + " VALUES(1,1,'Fixture Camera','',150000,'','active'),(2,1,'Price"
                            + " fixture','',200000,'','active'),(3,1,'Maintenance"
                            + " fixture','',100000,'','active')");
                s.executeUpdate(
                        "INSERT INTO devices(id,product_id,serial,status)"
                            + " VALUES(1,1,'TEST-ONE','available'),(2,1,'TEST-TWO','available'),(3,2,'TEST-THREE','available'),(4,3,'TEST-FOUR','maintenance')");
            }
            Dao dao = new Dao(con);
            MerchantBank bank = new MerchantBank("BIDV", "0000000000", "TEST FIXTURE");
            check(cart(1, 1, 1).getDiscount() == 0, "One day price");
            check(cart(1, 1, 3).getDiscount() == 10, "Three day discount");
            check(cart(1, 1, 5).getDiscount() == 15, "Five day discount");
            check(cart(1, 1, 7).getDiscount() == 20, "Seven day discount");
            int first = dao.place(1, cart(1, 1, 3), bank, new BigDecimal("405000"));
            check(
                    scalar(con, "SELECT total FROM rentals WHERE id=" + first) == 405000,
                    "Server total discount");
            check(
                    scalar(con, "SELECT COUNT(*) FROM rental_items WHERE rental_id=" + first) == 1,
                    "Device assigned");
            check(
                    scalar(
                                    con,
                                    "SELECT COUNT(*) FROM rental_payments WHERE rental_id="
                                            + first
                                            + " AND status='pending'")
                            == 1,
                    "Payment pending");
            check("pending".equals(dao.cartPaymentState(first, 1)), "Pending cart payment state");
            check(
                    "missing".equals(dao.cartPaymentState(first, 2)),
                    "Other owner cannot reconcile cart payment");
            check(dao.payment(first, 2) == null, "Other user cannot read payment");
            dao.report(first, 2);
            check(
                    "pending".equals(dao.payment(first, 1).get("status")),
                    "Other user cannot report payment");
            dao.report(first, 1);
            check(
                    "reported".equals(dao.payment(first, 1).get("status")),
                    "Customer report remains unverified");
            check(
                    "reported".equals(dao.cartPaymentState(first, 1)),
                    "Reported payment remains pending for cart");
            check(dao.confirmPaid(first), "Admin confirmation");
            check(
                    "paid".equals(dao.cartPaymentState(first, 1)),
                    "Only confirmed payment reconciles cart");
            check(!dao.confirmPaid(first), "Duplicate confirmation ignored");
            try {
                dao.place(1, cart(1, 2, 3), bank);
                throw new AssertionError("Overselling allowed");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    scalar(con, "SELECT COUNT(*) FROM rentals") == 1,
                    "No partial order on stock failure");
            int second = dao.place(1, cart(1, 1, 3), bank);
            check(
                    scalar(con, "SELECT COUNT(DISTINCT device_id) FROM rental_items") == 2,
                    "Distinct physical devices");
            try {
                dao.place(1, cart(1, 1, 3), bank);
                throw new AssertionError("Reserved device sold twice");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.place(1, cart(2, 1, 3), bank, BigDecimal.ONE);
                throw new AssertionError("Changed price accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.place(1, cart(3, 1, 3), bank);
                throw new AssertionError("Maintenance device assigned");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    scalar(con, "SELECT COUNT(*) FROM rentals") == 2
                            && scalar(con, "SELECT COUNT(*) FROM rental_payments") == 2,
                    "Failures rollback all records");
            var admin =
                    new data.dao.AdminRentalDao() {
                        protected Connection connection() {
                            return dao.connection();
                        }
                    };
            check(
                    admin.transition(first, "pending", "cancelled") != null,
                    "Paid order cannot be cancelled without refund handling");
            check(admin.transition(first, "pending", "confirmed") == null, "Approve paid order");
            check(admin.transition(first, "confirmed", "renting") == null, "Hand over paid order");
            check(
                    admin.transition(second, "pending", "confirmed") == null,
                    "Approve unpaid reservation");
            check(
                    admin.transition(second, "confirmed", "renting") != null,
                    "Reject handover before payment confirmation");
            check(
                    admin.transition(second, "confirmed", "cancelled") == null,
                    "Cancel unpaid order");
            check(
                    "cancelled".equals(dao.cartPaymentState(second, 1)),
                    "Cancelled order keeps cart contents");
            dao.report(second, 1);
            check(!dao.confirmPaid(second), "Cancelled order cannot be marked paid");
            check(
                    dao.payment(first, 1)
                            .get("qrPayload")
                            .equals(
                                    "00020101021238540010A00000072701240006970418011000000000000208QRIBFTTA530370454064050005802VN62120808LENSCAM163043301"),
                    "Offline QR matches official vietqr payload and CRC");
            int hold = dao.place(2, cart(2, 1, 3), bank);
            try (var s = con.createStatement()) {
                s.executeUpdate(
                        "UPDATE rental_payments SET created_at=CURRENT_TIMESTAMP - INTERVAL 29"
                            + " MINUTE WHERE rental_id="
                                + hold);
            }
            check(
                    "pending".equals(dao.cartPaymentState(hold, 2)),
                    "Hold remains valid before 30 minutes");
            try {
                dao.place(3, cart(2, 1, 3), bank);
                throw new AssertionError("Live hold reused");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try (var s = con.createStatement()) {
                s.executeUpdate(
                        "UPDATE rental_payments SET created_at=CURRENT_TIMESTAMP - INTERVAL 30"
                            + " MINUTE WHERE rental_id="
                                + hold);
            }
            check("cancelled".equals(dao.cartPaymentState(hold, 2)), "Hold expires at 30 minutes");
            dao.report(hold, 2);
            check(!dao.confirmPaid(hold), "Expired order cannot be reported or confirmed paid");
            check(
                    "pending".equals(dao.payment(hold, 2).get("status")),
                    "Late report leaves payment unverified");
            int replacement = dao.place(3, cart(2, 1, 3), bank);
            check(
                    scalar(con, "SELECT COUNT(*) FROM rental_items WHERE rental_id=" + replacement)
                            == 1,
                    "Expired hold releases last machine for another customer");
            dao.report(replacement, 3);
            try (var s = con.createStatement()) {
                s.executeUpdate(
                        "UPDATE rental_payments SET created_at=CURRENT_TIMESTAMP - INTERVAL 31"
                            + " MINUTE WHERE rental_id IN ("
                                + first
                                + ","
                                + replacement
                                + ")");
            }
            dao.expirePending();
            check(
                    "reported".equals(dao.cartPaymentState(replacement, 3)),
                    "Transfer reported in time remains reserved for reconciliation");
            check(
                    dao.confirmPaid(replacement),
                    "Reported transfer may be confirmed after deadline");
            check("paid".equals(dao.cartPaymentState(first, 1)), "Paid order never expires");
            int previousOpens = dao.opens, previousStatements = dao.prepared;
            var payment = dao.payment(replacement, 3);
            check(
                    dao.opens - previousOpens == 1 && dao.prepared - previousStatements == 2,
                    "Payment load uses one connection and two statements including expiration");
            check(
                    ((Number) payment.get("holdSeconds")).longValue() == 0,
                    "Combined query clamps elapsed hold time to zero");
            try (var s = con.createStatement()) {
                s.executeUpdate(
                        "INSERT INTO products(id,id_category,name,image,price,description,status)"
                            + " VALUES(4,1,'Canon EOS M10','',150000,'','active')");
                s.executeUpdate(
                        "INSERT INTO devices(id,product_id,serial,status)"
                            + " VALUES(5,4,'M10-FIXTURE','available')");
            }
            int direct = dao.place(4, cart(4, 1, 1), bank, new BigDecimal("150000"));
            var directPayment = dao.payment(direct, 4);
            check(
                    ((BigDecimal) directPayment.get("amount")).compareTo(new BigDecimal("150000"))
                            == 0,
                    "One Canon M10 for one day persists exactly 150000");
            check(
                    directPayment.get("qrPayload").toString().contains("5406150000"),
                    "VietQR amount encodes 150000 for selected machine");
            var customerOrders =
                    new data.dao.MyRentalDao() {
                        protected Connection connection() {
                            return dao.connection();
                        }
                    };
            check(
                    !customerOrders.cancelUnpaid(direct, 99),
                    "Another customer cannot cancel unpaid order");
            check(customerOrders.cancelUnpaid(direct, 4), "Customer cancels own unpaid order");
            check(
                    !customerOrders.cancelUnpaid(direct, 4),
                    "Repeated cancellation does not change history");
            check(
                    "cancelled".equals(dao.cartPaymentState(direct, 4)),
                    "Cancelled order remains in history");
            int next = dao.place(5, cart(4, 1, 1), bank, new BigDecimal("150000"));
            check(next != direct, "Cancellation releases last machine for next customer");
            dao.report(next, 5);
            check(
                    !customerOrders.cancelUnpaid(next, 5),
                    "Reported transfer cannot be cancelled by customer");
            dao.confirmPaid(next);
            check(
                    !customerOrders.cancelUnpaid(next, 5),
                    "Paid order cannot be cancelled by customer");
            try (var s = con.createStatement()) {
                s.executeUpdate("UPDATE rentals SET status='renting' WHERE id=" + next);
                s.executeUpdate(
                        "UPDATE rental_payments SET status='pending' WHERE rental_id=" + next);
            }
            check(
                    !customerOrders.cancelUnpaid(next, 5),
                    "Handed-over order cannot be cancelled even if payment data is pending");
            try (var s = con.createStatement()) {
                s.executeUpdate("UPDATE rentals SET status='confirmed' WHERE id=" + next);
                s.executeUpdate(
                        "INSERT INTO rental_receipts(rental_id,content_type,image_data) VALUES("
                                + next
                                + ",'image/png',X'00')");
            }
            check(
                    !customerOrders.cancelUnpaid(next, 5),
                    "Receipt prevents cancellation even if payment status is inconsistent");
            try (var s = con.createStatement()) {
                s.executeUpdate("DELETE FROM rental_receipts WHERE rental_id=" + next);
            }
            check(
                    customerOrders.cancelUnpaid(next, 5),
                    "Confirmed but unpaid and not handed-over order may be cancelled");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " checkout transaction checks using TEMPORARY tables only; no real orders"
                        + " or transfers.");
    }
}
