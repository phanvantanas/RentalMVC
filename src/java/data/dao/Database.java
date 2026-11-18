package data.dao;

import data.impl.CategoryImpl;
import data.impl.DeviceImpl;
import data.impl.ProductImpl;
import data.impl.RentalImpl;
import data.impl.RentalItemImpl;
import data.impl.UserImpl;

public class Database {
    public static UserDao getUserDao() {
        return new UserImpl();
    }

    public static CategoryDao getCategoryDao() {
        return new CategoryImpl();
    }

    public static ProductDao getProductDao() {
        return new ProductImpl();
    }

    public static DeviceDao getDeviceDao() {
        return new DeviceImpl();
    }

    public static RentalDao getRentalDao() {
        return new RentalImpl();
    }

    public static RentalItemDao getRentalItemDao() {
        return new RentalItemImpl();
    }
}
