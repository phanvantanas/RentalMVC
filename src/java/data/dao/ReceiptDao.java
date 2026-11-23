package data.dao;

import data.driver.MySQLDriver;

import service.ReceiptImage;

import java.sql.*;

public class ReceiptDao {
    public record Receipt(byte[] bytes, String contentType) {}

    protected Connection connection() throws SQLException {
        Connection c = MySQLDriver.getConnection();
        if (c == null) throw new SQLException("Receipt database unavailable");
        return c;
    }

    public void save(int rentalId, int userId, ReceiptImage.Validated image) throws SQLException {
        if (rentalId <= 0 || userId <= 0 || image == null)
            throw new IllegalArgumentException("Đơn thuê không hợp lệ.");
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement st =
                        c.prepareStatement(
                                "SELECT p.status,r.status AS rental_status,("
                                        + RentalHold.EXPIRED
                                        + ") AS hold_expired FROM rental_payments p JOIN rentals r"
                                        + " ON r.id=p.rental_id WHERE p.rental_id=? AND r.user_id=?"
                                        + " FOR UPDATE")) {
                    st.setInt(1, rentalId);
                    st.setInt(2, userId);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next())
                            throw new IllegalArgumentException("Không tìm thấy đơn thuê của bạn.");
                        if (rs.getBoolean("hold_expired"))
                            throw new IllegalArgumentException(
                                    "Đã hết 30 phút giữ máy. Không chuyển khoản thêm; nếu đã gửi"
                                        + " tiền, vui lòng liên hệ LensCam để đối soát.");
                        if ("paid".equals(rs.getString("status"))
                                || "cancelled".equals(rs.getString("rental_status")))
                            throw new IllegalArgumentException(
                                    "Đơn đã đối soát hoặc đã hủy, không thể thay ảnh chuyển"
                                        + " khoản.");
                    }
                }
                try (PreparedStatement st =
                        c.prepareStatement(
                                "INSERT INTO rental_receipts(rental_id,content_type,image_data)"
                                    + " VALUES(?,?,?) ON DUPLICATE KEY UPDATE"
                                    + " content_type=VALUES(content_type),image_data=VALUES(image_data),updated_at=CURRENT_TIMESTAMP")) {
                    st.setInt(1, rentalId);
                    st.setString(2, image.contentType());
                    st.setBytes(3, image.bytes());
                    st.executeUpdate();
                }
                try (PreparedStatement st =
                        c.prepareStatement(
                                "UPDATE rental_payments SET status='reported' WHERE rental_id=? AND"
                                    + " status='pending'")) {
                    st.setInt(1, rentalId);
                    st.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public Receipt find(int rentalId, int userId, boolean admin) throws SQLException {
        try (Connection c = connection();
                PreparedStatement st =
                        c.prepareStatement(
                                "SELECT b.content_type,b.image_data FROM rental_receipts b JOIN"
                                    + " rentals r ON r.id=b.rental_id WHERE b.rental_id=? AND"
                                    + " (r.user_id=? OR ?=1)")) {
            st.setInt(1, rentalId);
            st.setInt(2, userId);
            st.setInt(3, admin ? 1 : 0);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return new Receipt(rs.getBytes("image_data"), rs.getString("content_type"));
            }
        }
    }
}
