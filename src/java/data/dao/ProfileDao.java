package data.dao;

import data.driver.MySQLDriver;

import model.User;

import service.Passwords;

import java.sql.*;

public class ProfileDao {
    public static void validatePasswordChange(String current, String next, String confirm) {
        if (current == null || current.isEmpty() || current.length() > 128)
            throw new IllegalArgumentException(
                    "Vui lòng nhập mật khẩu hiện tại, tối đa 128 ký tự.");
        if (next == null || next.length() < 8 || next.length() > 128)
            throw new IllegalArgumentException("Mật khẩu mới phải từ 8 đến 128 ký tự.");
        if (!next.equals(confirm))
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        if (next.equals(current))
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
    }

    public void changePassword(int userId, String current, String next, String confirm)
            throws SQLException {
        validatePasswordChange(current, next, confirm);
        if (userId <= 0) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        try (Connection con = connection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT password FROM users WHERE id=? AND status='active' FOR"
                                    + " UPDATE")) {
                    st.setInt(1, userId);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next())
                            throw new IllegalArgumentException(
                                    "Tài khoản không còn hoạt động. Vui lòng đăng nhập lại.");
                        if (!Passwords.verify(current, rs.getString("password")))
                            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "UPDATE users SET password=? WHERE id=? AND status='active'")) {
                    st.setString(1, Passwords.hash(next));
                    st.setInt(2, userId);
                    if (st.executeUpdate() != 1) throw new SQLException("Password update failed");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) throw new SQLException("Profile database unavailable");
        return con;
    }

    /** The caller supplies only the authenticated session ID; privileges are never editable. */
    public User update(int userId, String name, String email, String phone) throws SQLException {
        AdminUserDao.validate(name, email, phone, "user", "active");
        if (userId <= 0) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại.");
        try (Connection con = connection()) {
            con.setAutoCommit(false);
            try {
                User user;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT id,role,created_at FROM users WHERE id=? AND"
                                    + " status='active' FOR UPDATE")) {
                    st.setInt(1, userId);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next())
                            throw new IllegalArgumentException(
                                    "Tài khoản không còn hoạt động. Vui lòng đăng nhập lại.");
                        user =
                                new User(
                                        userId,
                                        name,
                                        email,
                                        phone,
                                        null,
                                        rs.getString("role"),
                                        rs.getTimestamp("created_at"));
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT id FROM users WHERE id<>? AND (email=? OR phone=?)"
                                    + " LIMIT 1")) {
                    st.setInt(1, userId);
                    st.setString(2, email);
                    st.setString(3, phone);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next())
                            throw new IllegalArgumentException(
                                    "Email hoặc số điện thoại đã được sử dụng.");
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "UPDATE users SET name=?,email=?,phone=? WHERE id=? AND"
                                    + " status='active'")) {
                    st.setString(1, name);
                    st.setString(2, email);
                    st.setString(3, phone);
                    st.setInt(4, userId);
                    st.executeUpdate();
                }
                con.commit();
                return user;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}
