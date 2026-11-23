package security;

import controller.ProfileServlet;

import data.dao.ProfileDao;
import data.driver.MySQLDriver;

import jakarta.servlet.http.*;

import model.User;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class ProfileTest {

    static int checks;

    static void check(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static class Endpoint extends ProfileServlet {

        int savedId;
        String savedEmail, savedPhone;

        protected ProfileDao dao() {
            return new ProfileDao() {
                public User update(int id, String name, String email, String phone) {
                    savedId = id;
                    savedEmail = email;
                    savedPhone = phone;
                    return new User(id, name, email, phone, null, "user");
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
        Endpoint endpoint = new Endpoint();
        var guest = AdminSecurityTest.session(null, null);
        var state = new AdminSecurityTest.ResponseState();
        endpoint.get(AdminSecurityTest.request(guest, Map.of()), AdminSecurityTest.response(state));
        check(
                state.status == 302 && "/profile".equals(guest.getAttribute("afterLogin")),
                "Guest returns to profile after login");
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(guest, Map.of()), AdminSecurityTest.response(state));
        check(state.status == 401 && endpoint.savedId == 0, "Unauthenticated update rejected");
        User user = new User(7, "Old name", "old@example.test", "0900000000", null, "user");
        var session = AdminSecurityTest.session(user, null);
        session.setAttribute("shopCsrf", "expected");
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(session, Map.of("csrf", "wrong")),
                AdminSecurityTest.response(state));
        check(state.status == 403 && endpoint.savedId == 0, "CSRF rejected before save");
        state = new AdminSecurityTest.ResponseState();
        endpoint.post(
                AdminSecurityTest.request(
                        session,
                        Map.of(
                                "csrf",
                                "expected",
                                "id",
                                "999",
                                "role",
                                "admin",
                                "name",
                                "  Nguyễn Văn An  ",
                                "email",
                                " AN@EXAMPLE.TEST ",
                                "phone",
                                "090-123-4567")),
                AdminSecurityTest.response(state));
        check(endpoint.savedId == 7, "Target ID comes from authenticated session");
        check(
                "an@example.test".equals(endpoint.savedEmail)
                        && "0901234567".equals(endpoint.savedPhone),
                "Contact fields normalized");
        check(
                "user".equals(((User) session.getAttribute("user")).getRole())
                        && "An".equals(session.getAttribute("username")),
                "Session profile refreshed without accepting role");
        check(
                state.status == 302 && "/RentalCamera/profile".equals(state.location),
                "Successful save redirects to profile");
        if (args.length == 0) {
            System.out.println(
                    "PASS: "
                            + checks
                            + " profile access/session checks; pass db to test temporary database"
                            + " tables.");
            return;
        }
        try (Connection con = MySQLDriver.getConnection()) {
            if (con == null) {
                throw new AssertionError("Database unavailable");
            }
            try (Statement st = con.createStatement()) {
                st.execute(
                        "CREATE TEMPORARY TABLE users(id INT PRIMARY KEY,name VARCHAR(100),email"
                            + " VARCHAR(150) UNIQUE,phone VARCHAR(20),password VARCHAR(255),role"
                            + " VARCHAR(10),status VARCHAR(10),created_at TIMESTAMP DEFAULT"
                            + " CURRENT_TIMESTAMP) ENGINE=InnoDB");
                st.execute(
                        "INSERT INTO users(id,name,email,phone,password,role,status)"
                            + " VALUES(7,'Old','old@example.test','0900000000','original-hash','user','active'),(8,'Other','other@example.test','0911111111','other-hash','user','active'),(9,'Locked','locked@example.test','0922222222','locked-hash','user','locked')");
            }
            ProfileDao dao =
                    new ProfileDao() {
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
            User updated = dao.update(7, "Nguyễn Văn An", "an@example.test", "0901234567");
            check(
                    updated.getId() == 7 && updated.getPassword() == null,
                    "Returned principal has no password");
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE id=7")) {
                rs.next();
                check(
                        "Nguyễn Văn An".equals(rs.getString("name"))
                                && "an@example.test".equals(rs.getString("email"))
                                && "0901234567".equals(rs.getString("phone")),
                        "Contact fields persisted");
                check(
                        "original-hash".equals(rs.getString("password"))
                                && "user".equals(rs.getString("role"))
                                && "active".equals(rs.getString("status")),
                        "Password, role and account status preserved");
            }
            for (String[] fields :
                    List.of(
                            new String[] {"Other", "other@example.test", "0901234567"},
                            new String[] {"Other", "new@example.test", "0911111111"},
                            new String[] {"", "new@example.test", "0901234567"},
                            new String[] {"An", "bad-email", "0901234567"},
                            new String[] {"An", "new@example.test", "abc"})) {
                try {
                    dao.update(7, fields[0], fields[1], fields[2]);
                    throw new AssertionError("Invalid contact accepted");
                } catch (IllegalArgumentException expected) {
                    checks++;
                }
            }
            try {
                dao.update(9, "Locked", "locked@example.test", "0922222222");
                throw new AssertionError("Locked user updated");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT email FROM users WHERE id=7")) {
                rs.next();
                check(
                        "an@example.test".equals(rs.getString(1)),
                        "Rejected updates preserve saved profile");
            }
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT name FROM users WHERE id=8")) {
                rs.next();
                check("Other".equals(rs.getString(1)), "Other account unchanged");
            }
            try {
                dao.changePassword(7, "wrong-password", "New password 2026", "New password 2026");
                throw new AssertionError("Wrong password accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT password FROM users WHERE id=7")) {
                rs.next();
                check(
                        "original-hash".equals(rs.getString(1)),
                        "Wrong current password preserves credential");
            }
            dao.changePassword(
                    7, "original-hash", "  New password 2026  ", "  New password 2026  ");
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE id=7")) {
                rs.next();
                String hash = rs.getString("password");
                check(
                        hash.startsWith("pbkdf2-sha256$") && !hash.equals("  New password 2026  "),
                        "New password stored as salted hash");
                check(
                        service.Passwords.verify("  New password 2026  ", hash)
                                && !service.Passwords.verify("original-hash", hash),
                        "New password works and old password fails");
                check(
                        "an@example.test".equals(rs.getString("email"))
                                && "user".equals(rs.getString("role")),
                        "Password change preserves contact and permissions");
            }
            dao.changePassword(
                    7, "  New password 2026  ", "Another password 2026", "Another password 2026");
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT password FROM users WHERE id=7")) {
                rs.next();
                check(
                        service.Passwords.verify("Another password 2026", rs.getString(1)),
                        "Hashed current password supports subsequent changes");
            }
            try {
                dao.changePassword(9, "locked-hash", "New password 2026", "New password 2026");
                throw new AssertionError("Locked account changed password");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("SELECT password FROM users WHERE id=8")) {
                rs.next();
                check("other-hash".equals(rs.getString(1)), "Other account password unchanged");
            }
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " profile checks using TEMPORARY tables; no original data changed.");
    }
}
