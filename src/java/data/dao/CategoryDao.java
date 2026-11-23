package data.dao;

import model.Category;

import java.util.List;

public interface CategoryDao {
    List<Category> findAll();

    Category find(int id);

    boolean insert(String name);

    boolean update(int id, String newName);

    boolean delete(int id);
}
