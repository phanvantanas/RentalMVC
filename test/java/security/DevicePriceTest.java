package security;

import data.dao.AdminDeviceDao;
import data.driver.MySQLDriver;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class DevicePriceTest {

    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static BigDecimal price(Connection c, int id) throws SQLException {
        try (var s = c.prepareStatement("SELECT price FROM products WHERE id=?")) {
            s.setInt(1, id);
            try (var r = s.executeQuery()) {
                r.next();
                return r.getBigDecimal(1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = MySQLDriver.getConnection()) {
            try (var s = c.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE products(id INT PRIMARY KEY,price DECIMAL(12,2))"
                            + " ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE devices(id INT PRIMARY KEY,product_id INT)"
                            + " ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_items(device_id INT,price_per_day"
                            + " DECIMAL(12,2)) ENGINE=InnoDB");
                s.execute("INSERT INTO products VALUES(1,150000),(2,250000)");
                s.execute("INSERT INTO devices VALUES(1,1),(2,1),(3,2)");
                s.execute("INSERT INTO rental_items VALUES(1,150000)");
            }
            AdminDeviceDao dao =
                    new AdminDeviceDao() {
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
                                                    return m.invoke(c, a);
                                                } catch (InvocationTargetException e) {
                                                    throw e.getCause();
                                                }
                                            });
                        }
                    };
            dao.bulk(List.of(1), "price", null, new BigDecimal("350000.50"));
            check(
                    price(c, 1).compareTo(new BigDecimal("350000.50")) == 0,
                    "Exact daily price saved");
            check(
                    price(c, 2).compareTo(new BigDecimal("250000")) == 0,
                    "Unselected model unchanged");
            try (var s = c.createStatement();
                    var r =
                            s.executeQuery(
                                    "SELECT p.price FROM devices d JOIN products p ON"
                                        + " p.id=d.product_id WHERE d.id=2")) {
                r.next();
                check(
                        r.getBigDecimal(1).compareTo(new BigDecimal("350000.50")) == 0,
                        "All devices of same model use new price");
            }
            try (var s = c.createStatement();
                    var r =
                            s.executeQuery(
                                    "SELECT price_per_day FROM rental_items WHERE device_id=1")) {
                r.next();
                check(
                        r.getBigDecimal(1).compareTo(new BigDecimal("150000")) == 0,
                        "Existing rental price snapshot unchanged");
            }
            for (String value : List.of("-1", "10000000000", "1.234")) {
                try {
                    dao.bulk(List.of(1), "price", null, new BigDecimal(value));
                    throw new AssertionError("Invalid price accepted");
                } catch (IllegalArgumentException expected) {
                    checks++;
                }
            }
            try {
                dao.bulk(List.of(1, 999), "price", null, new BigDecimal("1"));
                throw new AssertionError("Missing device accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    price(c, 1).compareTo(new BigDecimal("350000.50")) == 0,
                    "Failed update preserves original price");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " daily-price checks with TEMPORARY tables; original prices unchanged.");
    }
}
