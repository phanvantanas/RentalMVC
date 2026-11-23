package data.dao;

import data.driver.MySQLDriver;

import model.AdminRental;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class AdminRentalDao {
    public List<AdminRental> findAll() throws SQLException {
        String sql =
                "SELECT r.id,r.status,r.start_date,r.end_date,r.total,r.late_fee,u.name AS"
                    + " customer_name,u.phone,u.email,p.name AS"
                    + " product_name,d.serial,ri.price_per_day,rp.status AS"
                    + " payment_status,rp.transfer_reference,rp.pickup_time,rp.return_time,EXISTS(SELECT"
                    + " 1 FROM rental_receipts proof WHERE proof.rental_id=r.id) AS has_receipt"
                    + " FROM rentals r LEFT JOIN users u ON u.id=r.user_id LEFT JOIN rental_items"
                    + " ri ON ri.rental_id=r.id LEFT JOIN devices d ON d.id=ri.device_id LEFT JOIN"
                    + " products p ON p.id=d.product_id LEFT JOIN rental_payments rp ON"
                    + " rp.rental_id=r.id ORDER BY r.id DESC,ri.id ASC";
        Map<Integer, AdminRental> rows = new LinkedHashMap<>();
        try (Connection con = connection();
                PreparedStatement st = con.prepareStatement(sql);
                ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                AdminRental row = rows.get(id);
                if (row == null) {
                    row =
                            new AdminRental(
                                    id,
                                    rs.getString("customer_name"),
                                    rs.getString("phone"),
                                    rs.getString("email"),
                                    rs.getString("status"),
                                    rs.getDate("start_date"),
                                    rs.getDate("end_date"),
                                    rs.getDouble("total"),
                                    rs.getDouble("late_fee"));
                    rows.put(id, row);
                    row.setPaymentStatus(rs.getString("payment_status"));
                    row.setTransferReference(rs.getString("transfer_reference"));
                    row.setPickupTime(rs.getString("pickup_time"));
                    row.setReturnTime(rs.getString("return_time"));
                    row.setReceiptAvailable(rs.getBoolean("has_receipt"));
                }
                if (rs.getString("product_name") != null)
                    row.getItems()
                            .add(
                                    new AdminRental.Line(
                                            rs.getString("product_name"),
                                            rs.getString("serial"),
                                            rs.getDouble("price_per_day")));
            }
        }
        return new ArrayList<>(rows.values());
    }

    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) throw new SQLException("Cannot connect to rental database");
        return con;
    }

    public String transition(int id, String expected, String next) throws SQLException {
        if ("returned".equals(next))
            return "Vui lòng dùng chức năng nhận lại máy và xác nhận giờ trả thực tế để tính phí"
                       + " trễ.";
        boolean allowed =
                ("pending".equals(expected) && Set.of("confirmed", "cancelled").contains(next))
                        || ("confirmed".equals(expected)
                                && Set.of("renting", "cancelled").contains(next))
                        || ("renting".equals(expected) && "returned".equals(next));
        if (!allowed) return "Thao tác không phù hợp với trạng thái đơn thuê.";
        try (Connection con = connection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);
            try {
                java.sql.Date start, end;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT status,start_date,end_date FROM rentals WHERE id=? FOR"
                                    + " UPDATE")) {
                    st.setInt(1, id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next() || !expected.equals(rs.getString("status"))) {
                            con.rollback();
                            return "Đơn đã thay đổi hoặc không tồn tại. Vui lòng tải lại.";
                        }
                        start = rs.getDate("start_date");
                        end = rs.getDate("end_date");
                    }
                }
                if ("cancelled".equals(next)) {
                    try (PreparedStatement st =
                            con.prepareStatement(
                                    "SELECT status FROM rental_payments WHERE rental_id=? FOR"
                                        + " UPDATE")) {
                        st.setInt(1, id);
                        try (ResultSet rs = st.executeQuery()) {
                            if (rs.next() && "paid".equals(rs.getString(1))) {
                                con.rollback();
                                return "Đơn đã nhận tiền. Cần xử lý hoàn tiền trước khi hủy.";
                            }
                        }
                    }
                }
                if ("renting".equals(next)) {
                    try (PreparedStatement st =
                            con.prepareStatement(
                                    "SELECT status FROM rental_payments WHERE rental_id=? FOR"
                                        + " UPDATE")) {
                        st.setInt(1, id);
                        try (ResultSet rs = st.executeQuery()) {
                            if (rs.next() && !"paid".equals(rs.getString(1))) {
                                con.rollback();
                                return "Cần đối soát và xác nhận đã nhận tiền trước khi bàn giao"
                                           + " thiết bị.";
                            }
                        }
                    }
                }
                if ("confirmed".equals(next) || "renting".equals(next)) {
                    if (start == null || end == null || end.before(start)) {
                        con.rollback();
                        return "Lịch thuê không hợp lệ.";
                    }
                    LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                    if (end.toLocalDate().isBefore(today)
                            || ("renting".equals(next) && start.toLocalDate().isAfter(today))) {
                        con.rollback();
                        return "Chỉ duyệt lịch còn hiệu lực và bàn giao khi đến ngày nhận.";
                    }
                    int devices = 0;
                    try (PreparedStatement st =
                            con.prepareStatement(
                                    "SELECT d.id,d.status,p.status AS product_status FROM devices d"
                                        + " JOIN products p ON p.id=d.product_id JOIN rental_items"
                                        + " ri ON ri.device_id=d.id WHERE ri.rental_id=? ORDER BY"
                                        + " d.id FOR UPDATE")) {
                        st.setInt(1, id);
                        try (ResultSet rs = st.executeQuery()) {
                            while (rs.next()) {
                                devices++;
                                if (!"available".equals(rs.getString("status"))
                                        || !"active".equals(rs.getString("product_status"))) {
                                    con.rollback();
                                    return "Thiết bị đang bảo trì hoặc ngừng cho thuê.";
                                }
                            }
                        }
                    }
                    if (devices == 0) {
                        con.rollback();
                        return "Đơn chưa có thiết bị để duyệt hoặc bàn giao.";
                    }
                    String overlap =
                            "SELECT 1 FROM rental_items mine JOIN rental_items other ON"
                                + " other.device_id=mine.device_id JOIN rentals r ON"
                                + " r.id=other.rental_id WHERE mine.rental_id=? AND r.id<>? AND"
                                + " r.status IN ('confirmed','renting') AND r.start_date<=? AND"
                                + " r.end_date>=? LIMIT 1";
                    try (PreparedStatement st = con.prepareStatement(overlap)) {
                        st.setInt(1, id);
                        st.setInt(2, id);
                        st.setDate(3, end);
                        st.setDate(4, start);
                        try (ResultSet rs = st.executeQuery()) {
                            if (rs.next()) {
                                con.rollback();
                                return "Thiết bị trùng lịch với đơn đã duyệt hoặc đang thuê.";
                            }
                        }
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "UPDATE rentals SET status=? WHERE id=? AND status=?")) {
                    st.setString(1, next);
                    st.setInt(2, id);
                    st.setString(3, expected);
                    if (st.executeUpdate() != 1) {
                        con.rollback();
                        return "Không thể cập nhật đơn. Vui lòng tải lại.";
                    }
                }
                con.commit();
                return null;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}
