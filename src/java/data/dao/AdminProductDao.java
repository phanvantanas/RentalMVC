package data.dao;

import data.driver.MySQLDriver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class AdminProductDao {

    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) {
            throw new SQLException("Product database unavailable");
        }
        return con;
    }

    public static void validate(
            int category,
            String name,
            String image,
            BigDecimal price,
            String description,
            String status) {
        if (category <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn danh mục sản phẩm.");
        }
        if (name == null
                || name.isBlank()
                || name.length() > 150
                || name.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Tên sản phẩm phải từ 1 đến 150 ký tự.");
        }
        if (image != null
                && !image.isEmpty()
                && (image.length() > 255
                        || image.contains("/")
                        || image.contains("\\")
                        || image.contains("..")
                        || image.chars().anyMatch(Character::isISOControl))) {
            throw new IllegalArgumentException("Ảnh phải là tên tệp hợp lệ trong thư mục ảnh.");
        }
        AdminDeviceDao.validatePrice(price);
        if (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new IllegalArgumentException("Mô tả quá dài, tối đa 65.535 byte UTF-8.");
        }
        if (!Set.of("active", "inactive").contains(status == null ? "" : status)) {
            throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ.");
        }
    }

    /** The product and its first physical device must be created together. */
    public int create(
            int category,
            String name,
            String image,
            BigDecimal price,
            String description,
            String status,
            String serial,
            String deviceStatus)
            throws SQLException {
        return create(category, "", name, image, price, description, status, serial, deviceStatus);
    }

    public int create(
            int category,
            String newCategory,
            String name,
            String image,
            BigDecimal price,
            String description,
            String status,
            String serial,
            String deviceStatus)
            throws SQLException {
        return createMany(
                category,
                newCategory,
                name,
                image,
                price,
                description,
                status,
                1,
                Collections.singletonList(serial),
                deviceStatus);
    }

    public int createMany(
            int category,
            String newCategory,
            String name,
            String image,
            BigDecimal price,
            String description,
            String status,
            int quantity,
            List<String> serials,
            String deviceStatus)
            throws SQLException {
        if (quantity < 1 || quantity > 100)
            throw new IllegalArgumentException("Số lượng nhập kho phải từ 1 đến 100 máy.");
        if (serials == null || serials.size() != quantity)
            throw new IllegalArgumentException("Số serial phải khớp với số lượng máy nhập kho.");
        serials = serials.stream().map(value -> value == null ? "" : value.strip()).toList();
        Set<String> unique = new HashSet<>();
        for (String serial : serials) {
            AdminDeviceDao.validateSerial(serial);
            if (!unique.add(serial.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Danh sách nhập kho có serial trùng nhau.");
        }
        newCategory = newCategory == null ? "" : newCategory.strip();
        if (newCategory.length() > 100 || newCategory.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Tên danh mục tối đa 100 ký tự và không chứa ký tự điều khiển.");
        }
        validate(newCategory.isEmpty() ? category : 1, name, image, price, description, status);
        AdminDeviceDao.validateStatus(deviceStatus);
        try (Connection con = connection()) {
            con.setAutoCommit(false);
            try {
                if (!newCategory.isEmpty()) {
                    try (PreparedStatement st =
                            con.prepareStatement(
                                    "INSERT INTO categories(name) VALUES(?) ON DUPLICATE KEY UPDATE"
                                        + " id=LAST_INSERT_ID(id)",
                                    Statement.RETURN_GENERATED_KEYS)) {
                        st.setString(1, newCategory);
                        st.executeUpdate();
                        try (ResultSet rs = st.getGeneratedKeys()) {
                            if (!rs.next()) {
                                throw new SQLException("Missing category ID");
                            }
                            category = rs.getInt(1);
                        }
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement("SELECT id FROM categories WHERE id=? FOR UPDATE")) {
                    st.setInt(1, category);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Danh mục không còn tồn tại. Vui lòng chọn lại.");
                        }
                    }
                }
                String slots = String.join(",", Collections.nCopies(quantity, "?"));
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT id FROM devices WHERE serial IN (" + slots + ")")) {
                    for (int i = 0; i < serials.size(); i++) st.setString(i + 1, serials.get(i));
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException("Số serial đã tồn tại trong kho.");
                        }
                    }
                }
                int id;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "INSERT INTO"
                                    + " products(id_category,name,image,price,description,status)"
                                    + " VALUES(?,?,?,?,?,?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, category);
                    st.setString(2, name);
                    st.setString(3, image == null || image.isBlank() ? null : image);
                    st.setBigDecimal(4, price);
                    st.setString(
                            5, description == null || description.isBlank() ? null : description);
                    st.setString(6, status);
                    if (st.executeUpdate() != 1) {
                        throw new SQLException("Product insert failed");
                    }
                    try (ResultSet rs = st.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Missing generated product ID");
                        }
                        id = rs.getInt(1);
                    }
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "INSERT INTO devices(product_id,serial,status) VALUES(?,?,?)")) {
                    st.setInt(1, id);
                    st.setString(3, deviceStatus);
                    for (String serial : serials) {
                        st.setString(2, serial);
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}
