package data.dao;

import data.driver.MySQLDriver;

import model.AdminDevice;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public class AdminDeviceDao {
    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) throw new SQLException("Device database unavailable");
        return con;
    }

    public List<AdminDevice> findAll() throws SQLException {
        String sql =
                "SELECT"
                    + " d.id,d.product_id,d.serial,d.status,p.id_category,p.name,p.image,p.price,p.status"
                    + " AS product_status,c.name AS category_name,(SELECT MAX(r.id) FROM"
                    + " rental_items ri JOIN rentals r ON r.id=ri.rental_id WHERE ri.device_id=d.id"
                    + " AND r.status='renting') AS rental_id,(SELECT COUNT(*) FROM rental_items ri"
                    + " JOIN rentals r ON r.id=ri.rental_id WHERE ri.device_id=d.id AND r.status IN"
                    + " ('pending','confirmed') AND r.start_date<=? AND r.end_date>=?) AS"
                    + " reservations FROM devices d JOIN products p ON p.id=d.product_id LEFT JOIN"
                    + " categories c ON c.id=p.id_category ORDER BY d.id DESC";
        List<AdminDevice> devices = new ArrayList<>();
        try (Connection con = connection();
                PreparedStatement st = con.prepareStatement(sql)) {
            java.sql.Date today =
                    java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            st.setDate(1, today);
            st.setDate(2, today);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next())
                    devices.add(
                            new AdminDevice(
                                    rs.getInt("id"),
                                    rs.getInt("product_id"),
                                    rs.getInt("id_category"),
                                    rs.getString("serial"),
                                    rs.getString("status"),
                                    rs.getString("name"),
                                    rs.getString("image"),
                                    rs.getString("product_status"),
                                    rs.getString("category_name"),
                                    rs.getBigDecimal("price"),
                                    rs.getInt("rental_id"),
                                    rs.getInt("reservations")));
            }
        }
        return devices;
    }

    public static void validateSerial(String serial) {
        if (serial == null
                || serial.isBlank()
                || serial.length() > 100
                || serial.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(
                    "Serial phải từ 1 đến 100 ký tự và không chứa ký tự điều khiển.");
    }

    public static void validateStatus(String status) {
        if (!Set.of("available", "maintenance").contains(status == null ? "" : status))
            throw new IllegalArgumentException("Trạng thái thiết bị không hợp lệ.");
    }

    public static void validatePrice(BigDecimal price) {
        if (price == null
                || price.signum() < 0
                || price.compareTo(new BigDecimal("9999999999.99")) > 0
                || price.scale() > 2)
            throw new IllegalArgumentException(
                    "Giá thuê phải từ 0 đến 9.999.999.999,99đ và có tối đa 2 số thập phân.");
    }

    public void save(Integer id, int productId, String serial, String status) throws SQLException {
        validateSerial(serial);
        validateStatus(status);
        if (productId <= 0 || (id != null && id <= 0))
            throw new IllegalArgumentException("Thiết bị hoặc mẫu máy không hợp lệ.");
        try (Connection con = connection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);
            try {
                if (id != null) {
                    lockDevices(con, List.of(id));
                    if (hasRental(con, List.of(id), false))
                        throw new IllegalArgumentException(
                                "Thiết bị có đơn chờ duyệt, đã duyệt hoặc đang thuê. Vui lòng xử lý"
                                    + " đơn trước khi sửa thiết bị.");
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT id FROM products WHERE id=? AND status='active' FOR"
                                    + " UPDATE")) {
                    st.setInt(1, productId);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next())
                            throw new IllegalArgumentException(
                                    "Mẫu máy không tồn tại hoặc đã ngừng cho thuê.");
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement("SELECT id FROM devices WHERE serial=? AND id<>?")) {
                    st.setString(1, serial);
                    st.setInt(2, id == null ? 0 : id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next())
                            throw new IllegalArgumentException("Số serial đã tồn tại trong kho.");
                    }
                }
                String sql =
                        id == null
                                ? "INSERT INTO devices(product_id,serial,status) VALUES(?,?,?)"
                                : "UPDATE devices SET product_id=?,serial=?,status=? WHERE id=?";
                try (PreparedStatement st = con.prepareStatement(sql)) {
                    st.setInt(1, productId);
                    st.setString(2, serial);
                    st.setString(3, status);
                    if (id != null) st.setInt(4, id);
                    if (st.executeUpdate() != 1) throw new SQLException("Device save failed");
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void bulk(List<Integer> ids, String action, String status, BigDecimal price)
            throws SQLException {
        if (ids == null
                || ids.isEmpty()
                || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new IllegalArgumentException("Chọn từ 1 đến 100 thiết bị hợp lệ.");
        if (!Set.of("status", "price", "delete").contains(action == null ? "" : action))
            throw new IllegalArgumentException("Thao tác không hợp lệ.");
        if ("status".equals(action)) validateStatus(status);
        if ("price".equals(action)) validatePrice(price);
        try (Connection con = connection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);
            try {
                List<Integer> productIds = lockDevices(con, ids);
                if (!"price".equals(action) && hasRental(con, ids, "delete".equals(action)))
                    throw new IllegalArgumentException(
                            "delete".equals(action)
                                    ? "Không thể xóa thiết bị đã có lịch sử đơn thuê."
                                    : "Thiết bị có lịch thuê đang hiệu lực. Vui lòng xử lý đơn"
                                          + " trước khi đổi trạng thái.");
                String sql;
                if ("price".equals(action))
                    sql =
                            "UPDATE products SET price=? WHERE id IN ("
                                    + placeholders(productIds.size())
                                    + ")";
                else if ("status".equals(action))
                    sql =
                            "UPDATE devices SET status=? WHERE id IN ("
                                    + placeholders(ids.size())
                                    + ")";
                else sql = "DELETE FROM devices WHERE id IN (" + placeholders(ids.size()) + ")";
                try (PreparedStatement st = con.prepareStatement(sql)) {
                    int index = 1;
                    if ("price".equals(action)) st.setBigDecimal(index++, price);
                    else if ("status".equals(action)) st.setString(index++, status);
                    for (int id : "price".equals(action) ? productIds : ids) st.setInt(index++, id);
                    st.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    private List<Integer> lockDevices(Connection con, List<Integer> ids) throws SQLException {
        Set<Integer> products = new TreeSet<>();
        int count = 0;
        try (PreparedStatement st =
                con.prepareStatement(
                        "SELECT id,product_id FROM devices WHERE id IN ("
                                + placeholders(ids.size())
                                + ") ORDER BY id FOR UPDATE")) {
            int index = 1;
            for (int id : ids) st.setInt(index++, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    count++;
                    products.add(rs.getInt("product_id"));
                }
            }
        }
        if (count != new HashSet<>(ids).size())
            throw new IllegalArgumentException(
                    "Một số thiết bị không còn tồn tại. Vui lòng tải lại.");
        return new ArrayList<>(products);
    }

    private boolean hasRental(Connection con, List<Integer> ids, boolean allHistory)
            throws SQLException {
        String sql =
                "SELECT 1 FROM rental_items ri JOIN rentals r ON r.id=ri.rental_id WHERE"
                    + " ri.device_id IN ("
                        + placeholders(ids.size())
                        + ")"
                        + (allHistory ? "" : " AND r.status IN ('pending','confirmed','renting')")
                        + " LIMIT 1";
        try (PreparedStatement st = con.prepareStatement(sql)) {
            int index = 1;
            for (int id : ids) st.setInt(index++, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
