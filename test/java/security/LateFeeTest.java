package security;

import data.dao.AdminRentalDao;
import data.dao.RentalReturnDao;
import data.driver.MySQLDriver;

import service.LateFeePolicy;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;

public class LateFeeTest {

    static int checks;

    static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
        checks++;
    }

    static int scalar(Connection con, String sql) throws SQLException {
        try (var s = con.createStatement();
                var r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    static Connection borrowed(Connection con) {
        return (Connection)
                Proxy.newProxyInstance(
                        LateFeeTest.class.getClassLoader(),
                        new Class<?>[] {Connection.class},
                        (p, m, a) -> {
                            if (m.getName().equals("close")) {
                                return null;
                            }
                            try {
                                return m.invoke(con, a);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
    }

    public static void main(String[] args) throws Exception {
        LocalDateTime due = LocalDateTime.of(2026, 1, 1, 17, 0);
        BigDecimal rate = new BigDecimal("300000");
        long[] seconds = {
            -60, 0, 3600, 3601, 10800, 10801, 21600, 21601, 43200, 43201, 86400, 90000, 90001,
            93600, 172800, 176401
        };
        int[] expected = {0, 0, 0, 10, 10, 20, 20, 40, 40, 70, 70, 70, 80, 80, 140, 150};
        for (int i = 0; i < seconds.length; i++) {
            var quote = LateFeePolicy.calculate(due, due.plusSeconds(seconds[i]), rate);
            check(
                    quote.percent() == expected[i]
                            && quote.fee()
                                            .compareTo(
                                                    rate.multiply(BigDecimal.valueOf(expected[i]))
                                                            .divide(BigDecimal.valueOf(100)))
                                    == 0,
                    "Boundary " + seconds[i]);
        }
        check(
                LateFeePolicy.calculate(due, due.plusHours(2), new BigDecimal("15.00"))
                        .fee()
                        .equals(new BigDecimal("2")),
                "VND half-up rounding");
        check(
                new AdminRentalDao().transition(1, "renting", "returned") != null,
                "Old return endpoint cannot bypass assessment");
        try (Connection con = MySQLDriver.getConnection()) {
            try (var s = con.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE users(id INT PRIMARY KEY,name VARCHAR(100),role"
                            + " VARCHAR(20),status VARCHAR(20)) ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rentals(id INT PRIMARY KEY,status"
                            + " VARCHAR(20),start_date DATE,end_date DATE,late_fee DECIMAL(12,2))"
                            + " ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_payments(rental_id INT PRIMARY"
                            + " KEY,return_time TIME) ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_items(rental_id INT,price_per_day"
                            + " DECIMAL(12,2)) ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_returns(rental_id INT PRIMARY KEY,due_at"
                            + " DATETIME,actual_return_at DATETIME,late_seconds BIGINT,daily_rate"
                            + " DECIMAL(12,2),charged_percent INT,calculated_fee"
                            + " DECIMAL(12,2),final_fee DECIMAL(12,2),adjustment_reason"
                            + " VARCHAR(500),received_by INT,received_at TIMESTAMP DEFAULT"
                            + " CURRENT_TIMESTAMP,fee_status VARCHAR(20),fee_paid_at"
                            + " DATETIME,fee_received_by INT,policy_version VARCHAR(40))"
                            + " ENGINE=InnoDB");
                s.execute(
                        "INSERT INTO users"
                            + " VALUES(1,'Admin','admin','active'),(2,'Customer','user','active'),(3,'Locked"
                            + " Admin','admin','locked')");
            }
            LocalDate end = LocalDate.now(LateFeePolicy.ZONE).minusDays(1);
            LocalDateTime deadline = end.atTime(17, 0), returned = deadline.plusHours(3);
            try (var st = con.prepareStatement("INSERT INTO rentals VALUES(?,'renting',?,?,0)")) {
                for (int id = 1; id <= 7; id++) {
                    st.setInt(1, id);
                    st.setDate(2, java.sql.Date.valueOf(end.minusDays(3)));
                    st.setDate(3, java.sql.Date.valueOf(end));
                    st.executeUpdate();
                }
            }
            try (var s = con.createStatement()) {
                s.execute(
                        "INSERT INTO rental_payments"
                            + " VALUES(1,'17:00'),(3,'17:00'),(4,'17:00'),(5,'17:00'),(6,'17:00'),(7,'17:00')");
                for (int id = 1; id <= 7; id++) {
                    s.execute(
                            "INSERT INTO rental_items VALUES("
                                    + id
                                    + ",300000),("
                                    + id
                                    + ",100000)");
                }
            }
            RentalReturnDao dao =
                    new RentalReturnDao() {
                        protected Connection connection() {
                            return borrowed(con);
                        }
                    };
            dao.receive(1, 1, returned, "01:00", null, "", false);
            check(
                    scalar(con, "SELECT late_fee FROM rentals WHERE id=1") == 40000,
                    "Fee uses sum of price snapshots and authoritative scheduled time");
            check(
                    scalar(con, "SELECT COUNT(*) FROM rentals WHERE id=1 AND status='returned'")
                            == 1,
                    "Return and fee committed together");
            check(
                    scalar(con, "SELECT late_seconds FROM rental_returns WHERE rental_id=1")
                            == 10800,
                    "Actual lateness stored");
            check(
                    scalar(con, "SELECT received_by FROM rental_returns WHERE rental_id=1") == 1,
                    "Receiving administrator audited");
            check(
                    scalar(
                                    con,
                                    "SELECT COUNT(*) FROM rental_returns WHERE rental_id=1 AND"
                                        + " fee_status='unpaid'")
                            == 1,
                    "Uncollected fee tracked independently");
            check(dao.confirmFee(1, 1), "Fee collection can be confirmed later");
            check(!dao.confirmFee(1, 1), "Fee collection confirmation is idempotent");
            try {
                dao.receive(1, 1, returned, "17:00", null, "", true);
                throw new AssertionError("Duplicate return");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(2, 1, returned, "17:00", BigDecimal.ZERO, "", false);
                throw new AssertionError("Waiver without reason");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            check(
                    scalar(con, "SELECT COUNT(*) FROM rental_returns WHERE rental_id=2") == 0
                            && scalar(
                                            con,
                                            "SELECT COUNT(*) FROM rentals WHERE id=2 AND"
                                                + " status='renting'")
                                    == 1,
                    "Invalid adjustment rolls back all return state");
            dao.receive(2, 1, returned, "17:00", BigDecimal.ZERO, "Khách đã báo trước", false);
            check(
                    scalar(
                                    con,
                                    "SELECT COUNT(*) FROM rental_returns WHERE rental_id=2 AND"
                                        + " fee_status='waived' AND calculated_fee=40000 AND"
                                        + " final_fee=0")
                            == 1,
                    "Legacy due time and waiver reason stored");
            try {
                dao.receive(3, 1, returned, "17:00", new BigDecimal("40001"), "", false);
                throw new AssertionError("Fee increase");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(
                        3,
                        1,
                        LocalDateTime.now(LateFeePolicy.ZONE).plusHours(1),
                        "17:00",
                        null,
                        "",
                        false);
                throw new AssertionError("Future return");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(3, 1, end.minusDays(4).atTime(12, 0), "17:00", null, "", false);
                throw new AssertionError("Return before rental");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(3, 2, returned, "17:00", null, "", false);
                throw new AssertionError("Customer received equipment");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(3, 3, returned, "17:00", null, "", false);
                throw new AssertionError("Locked admin received equipment");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.confirmFee(1, 2);
                throw new AssertionError("Customer confirmed fee");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            try {
                dao.receive(4, 1, returned, "17:00", new BigDecimal("10000.123"), "Reduce", false);
                throw new AssertionError("Excess precision");
            } catch (IllegalArgumentException expectedError) {
                checks++;
            }
            dao.receive(
                    4, 1, returned, "17:00", new BigDecimal("20000"), "Khách đã báo trước", true);
            check(
                    scalar(
                                    con,
                                    "SELECT COUNT(*) FROM rental_returns WHERE rental_id=4 AND"
                                        + " fee_status='paid' AND fee_received_by=1 AND"
                                        + " final_fee=20000")
                            == 1,
                    "Reduced fee can be recorded as collected with actor");
            dao.receive(5, 1, deadline.plusHours(1), "17:00", null, "", true);
            check(
                    scalar(con, "SELECT late_fee FROM rentals WHERE id=5") == 0
                            && scalar(
                                            con,
                                            "SELECT COUNT(*) FROM rental_returns WHERE rental_id=5"
                                                + " AND fee_status='waived'")
                                    == 1,
                    "One-hour grace never creates fee payment");
            check(
                    dao.findAllDetails().get(1).get("receiver_name").equals("Admin"),
                    "Audit details show receiving admin");
            try (var s = con.createStatement()) {
                s.execute("DELETE FROM rental_items WHERE rental_id=6");
                s.execute("UPDATE rental_items SET price_per_day=-1 WHERE rental_id=7");
            }
            con.commit();
            for (int id : new int[] {6, 7})
                try {
                    dao.receive(id, 1, returned, "17:00", null, "", false);
                    throw new AssertionError("Invalid price data accepted");
                } catch (IllegalArgumentException expectedError) {
                    checks++;
                }
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " late-fee boundaries/return transaction/audit checks with TEMPORARY"
                        + " tables; no original rental changed.");
    }
}
