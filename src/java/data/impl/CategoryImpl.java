package data.impl;

import data.dao.CategoryDao;
import data.driver.MySQLDriver;

import model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryImpl implements CategoryDao {
    private Connection getConnection() {
        return MySQLDriver.getConnection();
    }

    @Override
    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY id ASC";
        Connection con = getConnection();
        if (con == null) return list;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql);
                ResultSet rs = sttm.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException ex) {
            System.getLogger(CategoryImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return list;
    }

    @Override
    public Category find(int id) {
        String sql = "SELECT * FROM categories WHERE id=?";
        Connection con = getConnection();
        if (con == null) return null;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            try (ResultSet rs = sttm.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getInt("id"), rs.getString("name"));
                }
            }
        } catch (SQLException ex) {
            System.getLogger(CategoryImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }

    @Override
    public boolean insert(String name) {
        String sql = "INSERT INTO categories(name) VALUES(?)";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, name);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(CategoryImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean update(int id, String newName) {
        String sql = "UPDATE categories SET name=? WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setString(1, newName);
            sttm.setInt(2, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(CategoryImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM categories WHERE id=?";
        Connection con = getConnection();
        if (con == null) return false;

        try (con;
                PreparedStatement sttm = con.prepareStatement(sql)) {
            sttm.setInt(1, id);
            return sttm.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.getLogger(CategoryImpl.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return false;
    }
}
