package data.impl;

import data.dao.ProductDao;
import data.driver.MySQLDriver;

import model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductImpl implements ProductDao {
    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY id DESC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql);
                ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public List<Product> findByCategory(int id_category) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE id_category=? ORDER BY id DESC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id_category);
            try (ResultSet rs = sttm.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public Product find(int id) {
        String sql = "SELECT * FROM products WHERE id=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public boolean insert(Product product) {
        String sql =
                "INSERT INTO products(id_category, name, image, price, deposit, description,"
                    + " status) VALUES(?,?,?,?,?,?,?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, product.getId_category());
            sttm.setString(2, product.getName());
            sttm.setString(3, product.getImage());
            sttm.setDouble(4, product.getPrice());
            sttm.setDouble(5, product.getDeposit());
            sttm.setString(6, product.getDescription());
            sttm.setString(7, product.getStatus() != null ? product.getStatus() : "active");
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean update(Product product) {
        String sql =
                "UPDATE products SET id_category=?, name=?, image=?, price=?, deposit=?,"
                    + " description=?, status=? WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, product.getId_category());
            sttm.setString(2, product.getName());
            sttm.setString(3, product.getImage());
            sttm.setDouble(4, product.getPrice());
            sttm.setDouble(5, product.getDeposit());
            sttm.setString(6, product.getDescription());
            sttm.setString(7, product.getStatus());
            sttm.setInt(8, product.getId());
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(ProductImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int id_category = rs.getInt("id_category");
        String name = rs.getString("name");
        String image = rs.getString("image");
        double price = rs.getDouble("price");
        boolean depositConfigured = false;
        for (int column = 1; column <= rs.getMetaData().getColumnCount(); column++) {
            if ("deposit".equalsIgnoreCase(rs.getMetaData().getColumnLabel(column))) {
                depositConfigured = rs.getObject(column) != null;
                break;
            }
        }
        double deposit = depositConfigured ? rs.getDouble("deposit") : 0;
        String description = rs.getString("description");
        String status = rs.getString("status");
        Product product =
                new Product(id, id_category, name, image, price, deposit, description, status);
        product.setDepositConfigured(depositConfigured);
        return product;
    }
}
