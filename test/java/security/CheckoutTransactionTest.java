package security;
import model.RentalCart;import data.dao.CheckoutDao;import data.driver.MySQLDriver;import service.MerchantBank;import java.sql.*;import java.time.*;import java.math.BigDecimal;import java.lang.reflect.*;import java.nio.file.*;
public class CheckoutTransactionTest {
 static int checks;static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static class Dao extends CheckoutDao {final Connection con;Dao(Connection con){this.con=con;}protected Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{if(m.getName().equals("close")){if(!con.getAutoCommit())con.setAutoCommit(true);return null;}try{return m.invoke(con,a);}catch(InvocationTargetException e){throw e.getCause();}});}}
 static RentalCart cart(int product,int qty,int days){RentalCart cart=new RentalCart();LocalDate date=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));cart.add(product,qty,date,date.plusDays(days),"08:00","17:00");return cart;}
 static int scalar(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
 public static void main(String[]args)throws Exception{
  try(Connection con=MySQLDriver.getConnection()){
   if(con==null)throw new AssertionError("Database unavailable");
   String[] tables={
    "products(id INT PRIMARY KEY,id_category INT,name VARCHAR(150),image VARCHAR(255),price DECIMAL(12,2),description TEXT,status VARCHAR(20))",
    "devices(id INT PRIMARY KEY,product_id INT,serial VARCHAR(100),status VARCHAR(20))",
    "rentals(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,start_date DATE,end_date DATE,status VARCHAR(20),total DECIMAL(12,2),late_fee DECIMAL(12,2))",
    "rental_items(id INT AUTO_INCREMENT PRIMARY KEY,rental_id INT,device_id INT,price_per_day DECIMAL(12,2))",
    "rental_receipts(rental_id INT PRIMARY KEY,content_type VARCHAR(30),image_data MEDIUMBLOB,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
    "rental_payments(rental_id INT PRIMARY KEY,amount DECIMAL(12,2),transfer_reference VARCHAR(40) UNIQUE,bank_code VARCHAR(20),account_number VARCHAR(40),account_holder VARCHAR(120),pickup_time TIME,return_time TIME,status VARCHAR(20) DEFAULT 'pending')"};
   for(String definition:tables)try(var statement=con.createStatement()){statement.execute("CREATE TEMPORARY TABLE "+definition);}
   try(var s=con.createStatement()){s.executeUpdate("INSERT INTO products(id,id_category,name,image,price,description,status) VALUES(1,1,'Fixture Camera','',150000,'','active'),(2,1,'Price fixture','',200000,'','active'),(3,1,'Maintenance fixture','',100000,'','active')");s.executeUpdate("INSERT INTO devices(id,product_id,serial,status) VALUES(1,1,'TEST-ONE','available'),(2,1,'TEST-TWO','available'),(3,2,'TEST-THREE','available'),(4,3,'TEST-FOUR','maintenance')");}
   Dao dao=new Dao(con);MerchantBank bank=new MerchantBank("BIDV","0000000000","TEST FIXTURE");
   check(cart(1,1,1).getDiscount()==0,"One day price");check(cart(1,1,3).getDiscount()==10,"Three day discount");check(cart(1,1,5).getDiscount()==15,"Five day discount");check(cart(1,1,7).getDiscount()==20,"Seven day discount");
   int first=dao.place(1,cart(1,1,3),bank,new BigDecimal("405000"));check(scalar(con,"SELECT total FROM rentals WHERE id="+first)==405000,"Server total discount");check(scalar(con,"SELECT COUNT(*) FROM rental_items WHERE rental_id="+first)==1,"Device assigned");check(scalar(con,"SELECT COUNT(*) FROM rental_payments WHERE rental_id="+first+" AND status='pending'")==1,"Payment pending");
   check(dao.payment(first,2)==null,"Other user cannot read payment");dao.report(first,2);check("pending".equals(dao.payment(first,1).get("status")),"Other user cannot report payment");dao.report(first,1);check("reported".equals(dao.payment(first,1).get("status")),"Customer report remains unverified");check(dao.confirmPaid(first),"Admin confirmation");check(!dao.confirmPaid(first),"Duplicate confirmation ignored");
   try{dao.place(1,cart(1,2,3),bank);throw new AssertionError("Overselling allowed");}catch(IllegalArgumentException expected){checks++;}check(scalar(con,"SELECT COUNT(*) FROM rentals")==1,"No partial order on stock failure");
   int second=dao.place(1,cart(1,1,3),bank);check(scalar(con,"SELECT COUNT(DISTINCT device_id) FROM rental_items")==2,"Distinct physical devices");
   try{dao.place(1,cart(1,1,3),bank);throw new AssertionError("Reserved device sold twice");}catch(IllegalArgumentException expected){checks++;}
   try{dao.place(1,cart(2,1,3),bank,BigDecimal.ONE);throw new AssertionError("Changed price accepted");}catch(IllegalArgumentException expected){checks++;}
   try{dao.place(1,cart(3,1,3),bank);throw new AssertionError("Maintenance device assigned");}catch(IllegalArgumentException expected){checks++;}
   check(scalar(con,"SELECT COUNT(*) FROM rentals")==2&&scalar(con,"SELECT COUNT(*) FROM rental_payments")==2,"Failures rollback all records");
   var admin=new data.dao.AdminRentalDao(){protected Connection connection(){return dao.connection();}};
   check(admin.transition(first,"pending","cancelled")!=null,"Paid order cannot be cancelled without refund handling");
   check(admin.transition(first,"pending","confirmed")==null,"Approve paid order");
   check(admin.transition(first,"confirmed","renting")==null,"Hand over paid order");
   check(admin.transition(second,"pending","confirmed")==null,"Approve unpaid reservation");
   check(admin.transition(second,"confirmed","renting")!=null,"Reject handover before payment confirmation");
   check(admin.transition(second,"confirmed","cancelled")==null,"Cancel unpaid order");
   dao.report(second,1);check(!dao.confirmPaid(second),"Cancelled order cannot be marked paid");
   check(dao.payment(first,1).get("qrPayload").equals("00020101021238540010A00000072701240006970418011000000000000208QRIBFTTA530370454064050005802VN62120808LENSCAM163043301"),"Offline QR matches official vietqr payload and CRC");
  }
  System.out.println("PASS: "+checks+" checkout transaction checks using TEMPORARY tables only; no real orders or transfers.");
 }
}
