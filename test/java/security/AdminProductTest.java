package security;
import data.dao.AdminProductDao;
import data.driver.MySQLDriver;
import java.sql.*;
import java.math.BigDecimal;
import java.lang.reflect.*;

public class AdminProductTest {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    static void reject(Runnable action){try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        BigDecimal price=new BigDecimal("450000.50");
        reject(()->AdminProductDao.validate(1,"","a.jpg",price,"","active"));
        reject(()->AdminProductDao.validate(0,"Camera","a.jpg",price,"","active"));
        reject(()->AdminProductDao.validate(1,"Camera","../a.jpg",price,"","active"));
        reject(()->AdminProductDao.validate(1,"Camera","a.jpg",price,"","unknown"));
        reject(()->AdminProductDao.validate(1,"Camera","a.jpg",price,"界".repeat(22000),"active"));
        try(Connection con=MySQLDriver.getConnection()) {
            try(Statement s=con.createStatement()) {
                s.execute("CREATE TEMPORARY TABLE categories(id INT PRIMARY KEY) ENGINE=InnoDB");
                s.execute("CREATE TEMPORARY TABLE products(id INT AUTO_INCREMENT PRIMARY KEY,id_category INT,name VARCHAR(150),image VARCHAR(255),price DECIMAL(12,2),description TEXT,status VARCHAR(20)) ENGINE=InnoDB");
                s.execute("CREATE TEMPORARY TABLE devices(id INT AUTO_INCREMENT PRIMARY KEY,product_id INT,serial VARCHAR(100) UNIQUE,status VARCHAR(20),CHECK(serial <> 'FAIL')) ENGINE=InnoDB");
                s.execute("INSERT INTO categories VALUES(1)");
            }
            AdminProductDao dao=new AdminProductDao(){protected Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{if(m.getName().equals("close"))return null;try{return m.invoke(con,a);}catch(InvocationTargetException e){throw e.getCause();}});}};
            int id=dao.create(1,"Máy ảnh thử","a.jpg",price,"Mô tả thử","inactive","SERIAL-1","maintenance");
            try(Statement s=con.createStatement();ResultSet r=s.executeQuery("SELECT p.*,d.product_id,d.serial,d.status AS physical FROM products p JOIN devices d ON d.product_id=p.id")) {
                check(r.next()&&r.getInt("id")==id,"Generated ID linked to device");
                check(r.getInt("id_category")==1&&"Máy ảnh thử".equals(r.getString("name")),"Name and category stored");
                check("a.jpg".equals(r.getString("image"))&&price.equals(r.getBigDecimal("price")),"Image and exact price stored");
                check("Mô tả thử".equals(r.getString("description"))&&"inactive".equals(r.getString("status")),"Description and product status stored");
                check("SERIAL-1".equals(r.getString("serial"))&&"maintenance".equals(r.getString("physical")),"Serial and physical status stored");
            }
            try{dao.create(1,"Duplicate","",price,"","active","SERIAL-1","available");throw new AssertionError("Duplicate serial");}catch(IllegalArgumentException expected){checks++;}
            try{dao.create(99,"Bad category","",price,"","active","SERIAL-2","available");throw new AssertionError("Missing category");}catch(IllegalArgumentException expected){checks++;}
            try{dao.create(1,"Rollback","",price,"","active","FAIL","available");throw new AssertionError("Device failure");}catch(SQLException expected){checks++;}
            try(Statement s=con.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM products")){r.next();check(r.getInt(1)==1,"Failed device insert rolls back product");}
            dao.create(1,"Optional fields","",price,"","active","SERIAL-2","available");
            try(Statement s=con.createStatement();ResultSet r=s.executeQuery("SELECT image,description FROM products WHERE name='Optional fields'")){r.next();check(r.getString(1)==null&&r.getString(2)==null,"Nullable fields accepted");}
        }
        System.out.println("PASS: "+checks+" product checks using TEMPORARY tables; no original data changed.");
    }
}
