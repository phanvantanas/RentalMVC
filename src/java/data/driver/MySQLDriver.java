package data.driver;

import data.utils.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDriver {
    public static Connection getConnection() {
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }
            return DriverManager.getConnection(Constants.DB_URL, Constants.USER, Constants.PASS);
        } catch (ClassNotFoundException | SQLException ex) {
            System.getLogger(MySQLDriver.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return null;
    }
}
