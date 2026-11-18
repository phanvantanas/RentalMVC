package data.impl;

import data.dao.RentalDao;
import data.driver.MySQLDriver;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Rental;

public class RentalImpl implements RentalDao {
    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public List<Rental> findAll() {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rentals ORDER BY id DESC";
        Connection con = getConnection();
        if (con == null) return list;

        try (PreparedStatement sttm = con.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                list.add(mapRental(rs));
            }
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public List<Rental> findByUserId(int userId) {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rentals WHERE user_id=? ORDER BY id DESC";
        Connection con = getConnection();
        if (con == null) return list;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, userId);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRental(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public Rental find(int id) {
        String sql = "SELECT * FROM rentals WHERE id=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapRental(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public int insert(Rental rental) {
        String sql = "INSERT INTO rentals(user_id, start_date, end_date, status, total, deposit, late_fee) VALUES(?,?,?,?,?,?,?)";
        Connection con = getConnection();
        if (con == null) return -1;

        try (PreparedStatement sttm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sttm.setInt(1, rental.getUserId());
            sttm.setDate(2, rental.getStartDate());
            sttm.setDate(3, rental.getEndDate());
            sttm.setString(4, rental.getStatus() != null ? rental.getStatus() : "pending");
            sttm.setDouble(5, rental.getTotal());
            sttm.setDouble(6, rental.getDeposit());
            sttm.setDouble(7, rental.getLateFee());
            int affected = sttm.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = sttm.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return -1;
    }

    @Override
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE rentals SET status=? WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, status);
            sttm.setInt(2, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean update(Rental rental) {
        String sql = "UPDATE rentals SET start_date=?, end_date=?, status=?, total=?, deposit=?, late_fee=? WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setDate(1, rental.getStartDate());
            sttm.setDate(2, rental.getEndDate());
            sttm.setString(3, rental.getStatus());
            sttm.setDouble(4, rental.getTotal());
            sttm.setDouble(5, rental.getDeposit());
            sttm.setDouble(6, rental.getLateFee());
            sttm.setInt(7, rental.getId());
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(RentalImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    private Rental mapRental(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");
        String status = rs.getString("status");
        double total = rs.getDouble("total");
        double deposit = 0;
        for (int column = 1; column <= rs.getMetaData().getColumnCount(); column++) {
            if ("deposit".equalsIgnoreCase(rs.getMetaData().getColumnLabel(column))) {
                deposit = rs.getDouble(column);
                break;
            }
        }
        double lateFee = rs.getDouble("late_fee");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Rental(id, userId, startDate, endDate, status, total, deposit, lateFee, createdAt);
    }
}
