package data.dao;

import data.driver.MySQLDriver;
import java.sql.*;
import java.time.*;
import java.util.*;

public class MyRentalDao {
    protected Connection connection()throws SQLException {
        Connection con=MySQLDriver.getConnection();if(con==null)throw new SQLException("Rental database unavailable");return con;
    }
    public List<Map<String,Object>> findOwned(int userId,Integer orderId)throws SQLException {
        if(userId<=0||orderId!=null&&orderId<=0)throw new IllegalArgumentException("Invalid rental owner or order");
        String sql="SELECT r.id,r.status,r.start_date,r.end_date,r.total,r.late_fee,r.created_at,pay.status AS payment_status,pay.pickup_time,pay.return_time,pay.transfer_reference,EXISTS(SELECT 1 FROM rental_receipts proof WHERE proof.rental_id=r.id) AS has_receipt,p.name AS product_name,d.serial,ri.price_per_day FROM rentals r LEFT JOIN rental_payments pay ON pay.rental_id=r.id LEFT JOIN rental_items ri ON ri.rental_id=r.id LEFT JOIN devices d ON d.id=ri.device_id LEFT JOIN products p ON p.id=d.product_id WHERE r.user_id=?"+(orderId==null?"":" AND r.id=?")+" ORDER BY r.id DESC,ri.id";
        Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();
        try(Connection con=connection();PreparedStatement st=con.prepareStatement(sql)) {
            st.setInt(1,userId);if(orderId!=null)st.setInt(2,orderId);
            try(ResultSet rs=st.executeQuery()) {while(rs.next()) {
                int id=rs.getInt("id");Map<String,Object> row=rows.get(id);
                if(row==null) {
                    row=new HashMap<>();rows.put(id,row);row.put("id",id);row.put("status",rs.getString("status"));row.put("startDate",rs.getDate("start_date"));row.put("endDate",rs.getDate("end_date"));row.put("createdAt",rs.getTimestamp("created_at"));row.put("total",rs.getBigDecimal("total"));row.put("lateFee",rs.getBigDecimal("late_fee"));row.put("paymentStatus",rs.getString("payment_status"));row.put("hasPayment",rs.getString("payment_status")!=null);row.put("receiptAvailable",rs.getBoolean("has_receipt"));row.put("transferReference",rs.getString("transfer_reference"));
                    Time pickup=rs.getTime("pickup_time"),dropoff=rs.getTime("return_time");row.put("pickupTime",pickup==null?null:pickup.toString().substring(0,5));row.put("returnTime",dropoff==null?null:dropoff.toString().substring(0,5));
                    String status=rs.getString("status");int step=switch(status){case "pending"->1;case "confirmed"->2;case "renting"->3;case "returned"->4;default->0;};row.put("step",step);
                    java.sql.Date end=rs.getDate("end_date");row.put("overdue","renting".equals(status)&&end!=null&&end.toLocalDate().isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))));row.put("items",new ArrayList<Map<String,Object>>());
                }
                if(rs.getString("product_name")!=null) {
                    @SuppressWarnings("unchecked") List<Map<String,Object>> items=(List<Map<String,Object>>)row.get("items");
                    Map<String,Object> line=new HashMap<>();line.put("name",rs.getString("product_name"));line.put("serial",rs.getString("serial"));line.put("price",rs.getBigDecimal("price_per_day"));items.add(line);
                }
            }}
        }
        return new ArrayList<>(rows.values());
    }
}
