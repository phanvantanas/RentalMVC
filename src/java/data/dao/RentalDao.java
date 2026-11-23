package data.dao;

import model.Rental;

import java.util.List;

public interface RentalDao {
    List<Rental> findAll();

    List<Rental> findByUserId(int userId);

    Rental find(int id);

    int insert(Rental rental);

    boolean updateStatus(int id, String status);

    boolean update(Rental rental);
}
