package listener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@WebListener
public class DatabaseLifecycleListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ClassLoader applicationLoader = DatabaseLifecycleListener.class.getClassLoader();
        // Stop only this web application's Connector/J, preserving any shared server driver.
        if (AbandonedConnectionCleanupThread.class.getClassLoader() == applicationLoader) {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == applicationLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    event.getServletContext()
                            .log("Unable to deregister application JDBC driver", ex);
                }
            }
        }
    }
}
