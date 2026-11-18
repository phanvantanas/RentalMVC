package data.dao;

import java.util.List;
import model.Product;

public interface ProductDao {
    List<Product> findAll();
    List<Product> findByCategory(int id_category);
    Product find(int id);
    boolean insert(Product product);
    boolean update(Product product);
    boolean delete(int id);
}
