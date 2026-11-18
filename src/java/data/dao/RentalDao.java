package data.dao;

import java.util.List;
import model.Rental;

public interface RentalDao {
    List<Rental> findAll();
    List<Rental> findByUserId(int userId);
    Rental find(int id);
    int insert(Rental rental);
    boolean updateStatus(int id, String status);
    boolean update(Rental rental);
}
