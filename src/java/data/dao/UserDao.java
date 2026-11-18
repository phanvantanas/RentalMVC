package data.dao;

import java.util.List;
import model.User;

public interface UserDao {
    User findUser(String emailphone, String password);
    User findById(int id);
    boolean existsEmail(String email);
    boolean existsPhone(String phone);
    boolean insert(User user);
    List<User> findAll();
}
