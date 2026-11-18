package data.dao;

import java.util.List;
import model.Category;

public interface CategoryDao {
    List<Category> findAll();
    Category find(int id);
    boolean insert(String name);
    boolean update(int id, String newName);
    boolean delete(int id);
}
