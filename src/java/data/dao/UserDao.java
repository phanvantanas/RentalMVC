package data.dao;

import model.User;

import java.util.List;

public interface UserDao {
    User findUser(String emailphone, String password);

    User findById(int id);

    boolean existsEmail(String email);

    boolean existsPhone(String phone);

    boolean insert(User user);

    List<User> findAll();
}
