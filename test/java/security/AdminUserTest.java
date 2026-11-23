package security;

import data.dao.AdminUserDao;
import data.driver.MySQLDriver;
import data.impl.UserImpl;

import service.Passwords;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AdminUserTest {
    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    static void reject(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Invalid input accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    static Connection borrowed(Connection con) {
        return (Connection)
                Proxy.newProxyInstance(
                        AdminUserTest.class.getClassLoader(),
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

    static int count(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement();
                var r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    public static void main(String[] args) throws Exception {
        String hash = Passwords.hash("Fixture Password");
        check(
                !hash.equals("Fixture Password") && Passwords.verify("Fixture Password", hash),
                "New passwords are hashed and verifiable");
        check(!Passwords.verify("wrong", hash), "Wrong hash password rejected");
        check(!hash.equals(Passwords.hash("Fixture Password")), "Independent password salts");
        check(Passwords.verify("Legacy Fixture", "Legacy Fixture"), "Legacy passwords still work");
        check(
                !Passwords.verify("legacy fixture", "Legacy Fixture"),
                "Legacy passwords remain case sensitive");
        check(!Passwords.verify("test", "pbkdf2-sha256$broken"), "Malformed hashes rejected");
        reject(() -> Passwords.hash("short"));
        reject(() -> AdminUserDao.validate("", "a@example.com", "", "user", "active"));
        reject(() -> AdminUserDao.validate("Name", "invalid", "", "user", "active"));
        reject(() -> AdminUserDao.validate("Name", "a@example.com", "abc", "user", "active"));
        reject(() -> AdminUserDao.validate("Name", "a@example.com", "", "root", "active"));
        reject(() -> AdminUserDao.validate("Name", "a@example.com", "", "user", "unknown"));
        try (Connection c = MySQLDriver.getConnection()) {
            try (var s = c.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE users(id INT AUTO_INCREMENT PRIMARY KEY,name"
                            + " VARCHAR(100),email VARCHAR(150) UNIQUE,phone VARCHAR(20),password"
                            + " VARCHAR(255),role ENUM('admin','user'),status"
                            + " ENUM('active','locked') DEFAULT 'active',created_at TIMESTAMP"
                            + " DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rentals(id INT,user_id INT,status VARCHAR(20))"
                            + " ENGINE=InnoDB");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_payments(rental_id INT,status"
                            + " VARCHAR(20),amount DECIMAL(12,2)) ENGINE=InnoDB");
                s.execute(
                        "INSERT INTO users(id,name,email,phone,password,role,status)"
                            + " VALUES(1,'Admin One','admin1@example.test','0900000001','Legacy"
                            + " Fixture','admin','active'),(2,'Admin"
                            + " Two','admin2@example.test','0900000002','Legacy"
                            + " Fixture','admin','active'),(3,'Customer','customer@example.test','0900000003','Legacy"
                            + " Fixture','user','active'),(4,'Disposable','remove@example.test',NULL,'Legacy"
                            + " Fixture','user','active'),(5,'Locked','locked@example.test',NULL,'Legacy"
                            + " Fixture','user','locked')");
                s.execute("INSERT INTO rentals VALUES(1,3,'returned'),(2,3,'cancelled')");
                s.execute("INSERT INTO rental_payments VALUES(1,'paid',405000),(2,'paid',900000)");
            }
            AdminUserDao dao =
                    new AdminUserDao() {
                        protected Connection connection() {
                            return borrowed(c);
                        }
                    };
            var rows = dao.findAll();
            check(rows.size() == 5, "All managed users listed");
            check(
                    rows.stream().noneMatch(row -> row.containsKey("password")),
                    "Management list never contains passwords");
            var customer =
                    rows.stream()
                            .filter(row -> ((Number) row.get("id")).intValue() == 3)
                            .findFirst()
                            .orElseThrow();
            check(
                    ((Number) customer.get("rental_count")).intValue() == 2,
                    "Rental counts do not duplicate through payments");
            check(
                    new BigDecimal("405000.00").equals(customer.get("revenue")),
                    "Revenue excludes cancelled rentals");
            check(dao.activePrincipal(5) == null, "Locked principal rejected");
            check(
                    dao.activePrincipal(1).getPassword() == null,
                    "Refreshed session has no password");
            dao.save(
                    1,
                    null,
                    "Created User",
                    "created@example.test",
                    "0900000006",
                    "user",
                    "active",
                    "New Fixture Password");
            int created;
            try (var s = c.createStatement();
                    var r =
                            s.executeQuery(
                                    "SELECT id,password FROM users WHERE"
                                        + " email='created@example.test'")) {
                r.next();
                created = r.getInt(1);
                check(
                        !"New Fixture Password".equals(r.getString(2))
                                && Passwords.verify("New Fixture Password", r.getString(2)),
                        "Creation stores password hash");
            }
            UserImpl login =
                    new UserImpl() {
                        protected Connection getConnection() {
                            return borrowed(c);
                        }
                    };
            check(
                    login.findUser("created@example.test", "New Fixture Password") != null,
                    "New hashed account can sign in");
            check(
                    login.findUser("0900 000 003", "Legacy Fixture") != null,
                    "Existing phone login still works");
            check(
                    login.findUser("customer@example.test", "legacy fixture") == null,
                    "Existing wrong password rejected");
            check(
                    login.findUser("locked@example.test", "Legacy Fixture") == null,
                    "Locked user cannot authenticate");
            dao.save(
                    1,
                    created,
                    "Edited User",
                    "edited@example.test",
                    "0900000006",
                    "admin",
                    "active",
                    null);
            check(
                    "admin".equals(dao.activePrincipal(created).getRole()),
                    "Role and profile update applied");
            check(
                    login.findUser("edited@example.test", "New Fixture Password") != null,
                    "Profile editing preserves password");
            try {
                dao.save(
                        1,
                        null,
                        "Duplicate",
                        "customer@example.test",
                        "",
                        "user",
                        "active",
                        "Another Fixture");
                throw new AssertionError("Duplicate email accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.save(
                        1,
                        null,
                        "Duplicate Phone",
                        "new@example.test",
                        "0900000003",
                        "user",
                        "active",
                        "Another Fixture");
                throw new AssertionError("Duplicate phone accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.save(
                        1, 1, "Admin", "admin1@example.test", "0900000001", "user", "active", null);
                throw new AssertionError("Self-demotion accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            for (String action : List.of("lock", "delete")) {
                try {
                    dao.bulk(1, List.of(1), action);
                    throw new AssertionError("Self action accepted");
                } catch (IllegalArgumentException expected) {
                    checks++;
                }
            }
            try {
                dao.bulk(1, List.of(created), "lock");
                throw new AssertionError("Other admin locked");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(dao.activePrincipal(created) != null, "Other admin remains active");
            try {
                dao.save(
                        1,
                        2,
                        "Admin Two",
                        "admin2@example.test",
                        "0900000002",
                        "admin",
                        "locked",
                        null);
                throw new AssertionError("Admin locked through edit form");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    dao.activePrincipal(2) != null,
                    "Profile editing cannot lock another administrator");
            try {
                dao.bulk(1, List.of(3, 2), "lock");
                throw new AssertionError("Mixed admin lock selection accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    dao.activePrincipal(3) != null,
                    "Mixed bulk lock does not partially lock customers");
            dao.bulk(1, List.of(3), "lock");
            check(
                    dao.activePrincipal(3) == null
                            && login.findUser("customer@example.test", "Legacy Fixture") == null,
                    "Lock still blocks customer access");
            dao.bulk(1, List.of(3), "unlock");
            check(dao.activePrincipal(3) != null, "Unlock restores customer access");
            try {
                dao.bulk(1, List.of(4, 3), "delete");
                throw new AssertionError("Rental owner deleted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    count(c, "SELECT COUNT(*) FROM users WHERE id=4") == 1,
                    "Bulk delete failure preserves entire selection");
            try {
                dao.bulk(1, List.of(2), "delete");
                throw new AssertionError("Other admin deleted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.bulk(1, List.of(4, 2), "delete");
                throw new AssertionError("Mixed admin selection deleted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    count(c, "SELECT COUNT(*) FROM users WHERE id IN (2,4)") == 2,
                    "Admin-protected bulk delete changes no accounts");
            try {
                dao.save(
                        1,
                        2,
                        "Admin Two",
                        "admin2@example.test",
                        "0900000002",
                        "user",
                        "active",
                        null);
                throw new AssertionError("Admin demotion bypass accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    "admin".equals(dao.activePrincipal(2).getRole()),
                    "Administrator role cannot be downgraded to bypass delete protection");
            dao.bulk(1, List.of(4), "delete");
            check(
                    count(c, "SELECT COUNT(*) FROM users WHERE id=4") == 0,
                    "Unused account can be deleted");
            try (var statement = c.createStatement()) {
                statement.executeUpdate("UPDATE users SET status='locked' WHERE id=2");
            }
            c.commit();
            try {
                dao.save(
                        2,
                        3,
                        "Forbidden",
                        "customer@example.test",
                        "0900000003",
                        "admin",
                        "active",
                        null);
                throw new AssertionError("Locked admin wrote data");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    "user".equals(dao.activePrincipal(3).getRole()),
                    "Revoked admin cannot change another role");
            try {
                dao.bulk(3, List.of(1), "lock");
                throw new AssertionError("Customer wrote data");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(
                    dao.activePrincipal(1) != null
                            && "admin".equals(dao.activePrincipal(1).getRole()),
                    "Active administrator remains available");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " user management/password/access checks using TEMPORARY tables; original"
                        + " accounts unchanged.");
    }
}
