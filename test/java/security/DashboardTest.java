package security;

import data.dao.DashboardDao;
import data.driver.MySQLDriver;

import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DashboardTest {

    static int checks;

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = MySQLDriver.getConnection()) {
            if (con == null) {
                throw new IllegalStateException("Database unavailable");
            }
            try (Statement s = con.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE rentals(id INT,user_id INT,status VARCHAR(20),total"
                            + " DECIMAL(12,2),created_at DATETIME)");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_payments(rental_id INT,status"
                            + " VARCHAR(20),amount DECIMAL(12,2),updated_at DATETIME)");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_items(rental_id INT,device_id"
                            + " INT,price_per_day DECIMAL(12,2))");
                s.execute(
                        "CREATE TEMPORARY TABLE devices(id INT,product_id INT,status VARCHAR(20))");
                s.execute(
                        "CREATE TEMPORARY TABLE products(id INT,id_category INT,name"
                            + " VARCHAR(50),image VARCHAR(50))");
                s.execute("CREATE TEMPORARY TABLE categories(id INT,name VARCHAR(50))");
                s.execute("CREATE TEMPORARY TABLE users(id INT,name VARCHAR(50))");
                s.execute(
                        "INSERT INTO rentals VALUES(1,1,'renting',900,'2026-11-01"
                            + " 09:00:00'),(2,1,'pending',500,'2026-11-02'),(3,1,'cancelled',999,'2026-11-01'),(4,1,'returned',100,'2026-10-31')");
                s.execute(
                        "INSERT INTO rental_payments"
                            + " VALUES(1,'paid',900,'2026-11-02'),(2,'reported',500,'2026-11-02'),(3,'paid',999,'2026-11-01'),(4,'paid',100,'2026-10-31')");
                s.execute(
                        "INSERT INTO devices"
                            + " VALUES(1,1,'available'),(2,2,'available'),(3,1,'maintenance')");
                s.execute(
                        "INSERT INTO products VALUES(1,1,'Camera A','a.jpg'),(2,1,'Camera"
                            + " B','b.jpg')");
                s.execute("INSERT INTO categories VALUES(1,'Camera')");
                s.execute("INSERT INTO users VALUES(1,'Test customer')");
                s.execute(
                        "INSERT INTO rental_items"
                            + " VALUES(1,1,200),(1,2,100),(2,1,100),(3,2,100),(4,2,100)");
            }
            DashboardDao dao =
                    new DashboardDao() {
                        @Override
                        protected Connection connection() {
                            return (Connection)
                                    Proxy.newProxyInstance(
                                            getClass().getClassLoader(),
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
                    };
            Map<String, Object> data =
                    dao.load(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));
            Map<?, ?> money = (Map<?, ?>) data.get("money"),
                    orders = (Map<?, ?>) data.get("orders"),
                    fleet = (Map<?, ?>) data.get("fleet");
            check(
                    ((Number) money.get("revenue")).intValue() == 900,
                    "Only paid non-cancelled amounts in range");
            check(((Number) money.get("payments")).intValue() == 1, "One confirmed payment");
            check(
                    ((Number) orders.get("total")).intValue() == 3,
                    "Orders counted once including cancelled");
            check(((Number) orders.get("pending")).intValue() == 1, "Pending count");
            check(((Number) fleet.get("total")).intValue() == 3, "Fleet total");
            check(
                    ((Number) fleet.get("busy")).intValue() == 2,
                    "Distinct physical devices renting");
            check(((Number) fleet.get("maintenance")).intValue() == 1, "Maintenance count");
            List<?> days = (List<?>) data.get("days");
            check(days.size() == 3, "Zero days retained");
            check(
                    ((Number) ((Map<?, ?>) days.get(0)).get("revenue")).intValue() == 0,
                    "Revenue uses payment date");
            check(
                    ((Number) ((Map<?, ?>) days.get(1)).get("revenue")).intValue() == 900,
                    "Revenue date bucket");
            List<?> top = (List<?>) data.get("top");
            check(top.size() == 2, "Product ranking excludes other period and cancelled");
            check(
                    ((Number) ((Map<?, ?>) top.get(0)).get("value_total")).intValue() == 1100,
                    "Allocated totals avoid double-counting multidevice rental");
            check(((List<?>) data.get("activity")).size() == 3, "Recent orders filtered by period");
            Map<String, Object> empty =
                    dao.load(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1));
            check(
                    ((Number) ((Map<?, ?>) empty.get("money")).get("revenue")).intValue() == 0,
                    "Empty report has zero revenue");
            check(((List<?>) empty.get("top")).isEmpty(), "Empty product ranking");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " dashboard checks using TEMPORARY tables; original data unchanged.");
    }
}
