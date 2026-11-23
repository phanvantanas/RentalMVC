package data.dao;

import model.RentalItem;

import java.util.List;

public interface RentalItemDao {
    List<RentalItem> findByRentalId(int rentalId);

    boolean insert(RentalItem item);

    boolean insertBatch(List<RentalItem> items);
}
