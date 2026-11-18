package data.impl;

import data.dao.DeviceDao;
import data.driver.MySQLDriver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import model.Device;

public class DeviceImpl implements DeviceDao {
    @Override
    public Map<Integer, Integer> countAvailable(LocalDate start, LocalDate end) {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT d.product_id, COUNT(*) AS available_count FROM devices d "
                + "WHERE d.status='available' AND NOT EXISTS ("
                + "SELECT 1 FROM rental_items ri JOIN rentals r ON r.id=ri.rental_id "
                + "WHERE ri.device_id=d.id AND r.status IN ('pending','confirmed','renting') "
                + "AND r.start_date <= ? AND r.end_date >= ?) GROUP BY d.product_id";
        try (Connection con = getConnection()) {
            if (con == null) return counts;
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setDate(1, java.sql.Date.valueOf(end));
                statement.setDate(2, java.sql.Date.valueOf(start));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) counts.put(rs.getInt("product_id"), rs.getInt("available_count"));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, "Cannot load availability", ex);
        }
        return counts;
    }

    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public List<Device> findAll() {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices ORDER BY id ASC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con; PreparedStatement sttm = con.prepareStatement(sql);
             ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                list.add(mapDevice(rs));
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public List<Device> findByProductId(int productId) {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE product_id=? ORDER BY id ASC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, productId);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDevice(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public List<Device> findAvailableByProductId(int productId) {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE product_id=? AND status='available' ORDER BY id ASC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, productId);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDevice(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public Device find(int id) {
        String sql = "SELECT * FROM devices WHERE id=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapDevice(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public Device findBySerial(String serial) {
        String sql = "SELECT * FROM devices WHERE serial=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, serial);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapDevice(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public boolean insert(Device device) {
        String sql = "INSERT INTO devices(product_id, serial, status) VALUES(?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, device.getProductId());
            sttm.setString(2, device.getSerial());
            sttm.setString(3, device.getStatus() != null ? device.getStatus() : "available");
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE devices SET status=? WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, status);
            sttm.setInt(2, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM devices WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con; PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(DeviceImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    private Device mapDevice(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int productId = rs.getInt("product_id");
        String serial = rs.getString("serial");
        String status = rs.getString("status");
        return new Device(id, productId, serial, status);
    }
}
