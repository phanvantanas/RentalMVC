package security;

import controller.ReceiptServlet;

import data.dao.ReceiptDao;
import data.driver.MySQLDriver;

import jakarta.servlet.http.*;

import service.ReceiptImage;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import javax.imageio.ImageIO;

public class ReceiptTest {

    static int checks;

    static void check(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
        checks++;
    }

    static byte[] image(String format, int width) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, 2, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    static void reject(byte[] value) throws Exception {
        try {
            ReceiptImage.validate(value);
            throw new AssertionError("Invalid image accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    static class Endpoint extends ReceiptServlet {

        void get(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doGet(q, s);
        }

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    public static void main(String[] args) throws Exception {
        var png = ReceiptImage.validate(image("png", 3));
        var jpg = ReceiptImage.validate(image("jpg", 3));
        check("image/png".equals(png.contentType()), "PNG accepted");
        check("image/jpeg".equals(jpg.contentType()), "JPEG accepted");
        reject(new byte[0]);
        reject(new byte[ReceiptImage.MAX_BYTES + 1]);
        reject("<svg onload='alert(1)'/>".getBytes());
        reject(image("png", 6001));
        var extra = new ByteArrayOutputStream();
        extra.write(image("png", 3));
        extra.write("<script>not-an-image</script>".getBytes());
        var clean = ReceiptImage.validate(extra.toByteArray());
        check(
                !new String(clean.bytes(), java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains("<script>"),
                "Trailing payload removed");
        var state = new AdminSecurityTest.ResponseState();
        new Endpoint()
                .get(
                        AdminSecurityTest.request(AdminSecurityTest.session(null, null), Map.of()),
                        AdminSecurityTest.response(state));
        check(state.status == 401, "Guest cannot view receipt");
        state = new AdminSecurityTest.ResponseState();
        new Endpoint()
                .post(
                        AdminSecurityTest.request(AdminSecurityTest.session(null, null), Map.of()),
                        AdminSecurityTest.response(state));
        check(state.status == 401, "Guest cannot upload");
        state = new AdminSecurityTest.ResponseState();
        new Endpoint()
                .post(
                        AdminSecurityTest.request(
                                AdminSecurityTest.session(AdminSecurityTest.user("user"), null),
                                Map.of()),
                        AdminSecurityTest.response(state));
        check(state.status == 403, "Receipt CSRF");
        try (Connection c = MySQLDriver.getConnection()) {
            try (var s = c.createStatement()) {
                s.execute(
                        "CREATE TEMPORARY TABLE rentals(id INT PRIMARY KEY,user_id INT,status"
                            + " VARCHAR(20))");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_payments(rental_id INT PRIMARY KEY,status"
                            + " VARCHAR(20),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                s.execute(
                        "CREATE TEMPORARY TABLE rental_receipts(rental_id INT PRIMARY"
                            + " KEY,content_type VARCHAR(30),image_data MEDIUMBLOB,updated_at"
                            + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                s.executeUpdate(
                        "INSERT INTO rentals"
                            + " VALUES(1,1,'pending'),(2,2,'pending'),(3,1,'cancelled'),(4,1,'confirmed')");
                s.executeUpdate(
                        "INSERT INTO rental_payments(rental_id,status)"
                            + " VALUES(1,'pending'),(2,'pending'),(3,'pending'),(4,'paid')");
            }
            ReceiptDao dao =
                    new ReceiptDao() {
                        protected Connection connection() {
                            return (Connection)
                                    Proxy.newProxyInstance(
                                            getClass().getClassLoader(),
                                            new Class<?>[] {Connection.class},
                                            (p, m, a) -> {
                                                if (m.getName().equals("close")) {
                                                    if (!c.getAutoCommit()) {
                                                        c.setAutoCommit(true);
                                                    }
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
            dao.save(1, 1, png);
            check(dao.find(1, 1, false) != null, "Owner can read receipt");
            check(dao.find(1, 2, false) == null, "Other customer cannot read");
            check(dao.find(1, 2, true) != null, "Admin can read");
            try {
                dao.save(1, 2, png);
                throw new AssertionError("Other customer changed receipt");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.save(3, 1, png);
                throw new AssertionError("Cancelled order upload");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            try {
                dao.save(4, 1, png);
                throw new AssertionError("Paid receipt overwritten");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            dao.save(1, 1, jpg);
            check(
                    "image/jpeg".equals(dao.find(1, 1, false).contentType()),
                    "Unverified receipt can be replaced");
            try (var s = c.createStatement();
                    var r =
                            s.executeQuery(
                                    "SELECT status FROM rental_payments WHERE rental_id=1")) {
                r.next();
                check("reported".equals(r.getString(1)), "Image does not mark paid");
            }
            try (var s = c.createStatement()) {
                s.executeUpdate("INSERT INTO rentals VALUES(5,1,'pending')");
                s.executeUpdate(
                        "INSERT INTO rental_payments(rental_id,status,created_at)"
                            + " VALUES(5,'pending',CURRENT_TIMESTAMP - INTERVAL 30 MINUTE)");
            }
            try {
                dao.save(5, 1, png);
                throw new AssertionError("Expired hold accepts receipt");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
            check(dao.find(5, 1, false) == null, "Expired upload does not store receipt");
        }
        System.out.println(
                "PASS: "
                        + checks
                        + " image/upload/privacy checks using TEMPORARY tables; no real receipts"
                        + " stored.");
    }
}
