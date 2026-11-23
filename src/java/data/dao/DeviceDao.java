package data.dao;

import model.Device;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DeviceDao {
    Map<Integer, Integer> countAvailable(LocalDate start, LocalDate end);

    List<Device> findAll();

    List<Device> findByProductId(int productId);

    List<Device> findAvailableByProductId(int productId);

    Device find(int id);

    Device findBySerial(String serial);

    boolean insert(Device device);

    boolean updateStatus(int id, String status);

    boolean delete(int id);
}
