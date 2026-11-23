package data.impl;

import data.dao.RentalItemDao;
import data.driver.MySQLDriver;

import model.RentalItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, rentalId);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRentalItem(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(RentalItemImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public boolean insert(RentalItem item) {
        String sql = "INSERT INTO rental_items(rental_id, device_id, price_per_day) VALUES(?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, item.getRentalId());
            sttm.setInt(2, item.getDeviceId());
            sttm.setDouble(3, item.getPricePerDay());
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(RentalItemImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean insertBatch(List<RentalItem> items) {
        if (items == null || items.isEmpty()) return true;
        String sql = "INSERT INTO rental_items(rental_id, device_id, price_per_day) VALUES(?,?,?)";
        try (Connection con = getConnection()) {
            if (con == null) return false;
            con.setAutoCommit(false);
            try {
                try (PreparedStatement statement = con.prepareStatement(sql)) {
                    for (RentalItem item : items) {
                        statement.setInt(1, item.getRentalId());
                        statement.setInt(2, item.getDeviceId());
                        statement.setDouble(3, item.getPricePerDay());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                System.getLogger(RentalItemImpl.class.getName())
                        .log(System.Logger.Level.ERROR, "Cannot save rental items", ex);
                return false;
            }
        } catch (SQLException ex) {
            System.getLogger(RentalItemImpl.class.getName())
                    .log(System.Logger.Level.ERROR, "Cannot close rental connection", ex);
            return false;
        }
    }

    private RentalItem mapRentalItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int rentalId = rs.getInt("rental_id");
        int deviceId = rs.getInt("device_id");
        double pricePerDay = rs.getDouble("price_per_day");
        return new RentalItem(id, rentalId, deviceId, pricePerDay);
    }
}
