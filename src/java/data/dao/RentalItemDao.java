package data.dao;

import java.util.List;
import model.RentalItem;

public interface RentalItemDao {
    List<RentalItem> findByRentalId(int rentalId);
    boolean insert(RentalItem item);
    boolean insertBatch(List<RentalItem> items);
}
