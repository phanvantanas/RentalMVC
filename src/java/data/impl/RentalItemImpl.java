package data.impl;

import data.dao.RentalItemDao;
import data.driver.MySQLDriver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RentalItem;

public class RentalItemImpl implements RentalItemDao {
    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public List<RentalItem> findByRentalId(int rentalId) {
        List<RentalItem> list = new ArrayList<>();
        String sql = "SELECT * FROM rental_items WHERE rental_id=? ORDER BY id ASC";
        Connection con = getConnection();
        if (con == null) return list;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, rentalId);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRentalItem(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(RentalItemImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public boolean insert(RentalItem item) {
        String sql = "INSERT INTO rental_items(rental_id, device_id, price_per_day) VALUES(?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, item.getRentalId());
            sttm.setInt(2, item.getDeviceId());
            sttm.setDouble(3, item.getPricePerDay());
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(RentalItemImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean insertBatch(List<RentalItem> items) {
        if (items == null || items.isEmpty()) return true;
        String sql = "INSERT INTO rental_items(rental_id, device_id, price_per_day) VALUES(?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try {
            con.setAutoCommit(false);
            try (PreparedStatement sttm = con.prepareStatement(sql)) {
                for (RentalItem item : items) {
                    sttm.setInt(1, item.getRentalId());
                    sttm.setInt(2, item.getDeviceId());
                    sttm.setDouble(3, item.getPricePerDay());
                    sttm.addBatch();
                }
                sttm.executeBatch();
            }
            con.commit();
            con.setAutoCommit(true);
            return true;
        } catch (SQLException ex) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException e) {
                // ignore
            }
            System.getLogger(RentalItemImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    private RentalItem mapRentalItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int rentalId = rs.getInt("rental_id");
        int deviceId = rs.getInt("device_id");
        double pricePerDay = rs.getDouble("price_per_day");
        return new RentalItem(id, rentalId, deviceId, pricePerDay);
    }
}
