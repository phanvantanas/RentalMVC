package data.impl;

import data.dao.UserDao;
import data.driver.MySQLDriver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.User;

public class UserImpl implements UserDao {
    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public User findUser(String emailphone, String password) {
        String sql;
        if (emailphone != null && emailphone.contains("@")) {
            sql = "SELECT * FROM users WHERE email=? AND password=?";
        } else {
            sql = "SELECT * FROM users WHERE phone=? AND password=?";
        }

        Connection con = getConnection();
        if (con == null) return null;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, emailphone);
            sttm.setString(2, password);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(UserImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(UserImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    private boolean exists(String column, String value) {
        String sql = "SELECT 1 FROM users WHERE " + column + "=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, value);
            try (ResultSet rs = sttm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.getLogger(UserImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean existsEmail(String email) {
        return exists("email", email);
    }

    @Override
    public boolean existsPhone(String phone) {
        return exists("phone", phone);
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO users(name, email, phone, password, role) VALUES(?,?,?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, user.getName());
            sttm.setString(2, user.getEmail());
            sttm.setString(3, user.getPhone());
            sttm.setString(4, user.getPassword());
            sttm.setString(5, user.getRole() != null ? user.getRole() : "customer");
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(UserImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id DESC";
        Connection con = getConnection();
        if (con == null) return list;

        try (PreparedStatement sttm = con.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException ex) {
            System.getLogger(UserImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String pass = rs.getString("password");
        String role = rs.getString("role");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new User(id, name, email, phone, pass, role, createdAt);
    }
}
