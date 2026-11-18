package data.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Read-only operational reporting; confirmed payments are recognized on confirmation date. */
public class DashboardDao extends AdminRentalDao {
    private List<Map<String,Object>> query(Connection con, String sql, LocalDate from, LocalDate until) throws SQLException {
        try (PreparedStatement st = con.prepareStatement(sql)) {
            if (from != null) { st.setDate(1, java.sql.Date.valueOf(from)); st.setDate(2, java.sql.Date.valueOf(until)); }
            try (ResultSet rs = st.executeQuery()) {
                List<Map<String,Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1;i<=rs.getMetaData().getColumnCount();i++) row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public Map<String,Object> load(LocalDate from, LocalDate to) throws SQLException {
        try (Connection con = connection()) {
            LocalDate until = to.plusDays(1);
            Map<String,Object> data = new HashMap<>();
            data.put("money",query(con,"SELECT COALESCE(SUM(rp.amount),0) AS revenue,COUNT(*) AS payments FROM rental_payments rp JOIN rentals r ON r.id=rp.rental_id WHERE rp.status='paid' AND r.status<>'cancelled' AND rp.updated_at>=? AND rp.updated_at<?",from,until).get(0));
            data.put("orders",query(con,"SELECT COUNT(*) AS total,COALESCE(SUM(status='pending'),0) AS pending,COALESCE(SUM(status='confirmed'),0) AS confirmed,COALESCE(SUM(status='renting'),0) AS renting FROM rentals WHERE created_at>=? AND created_at<?",from,until).get(0));
            data.put("fleet",query(con,"SELECT COUNT(*) AS total,COALESCE(SUM(EXISTS(SELECT 1 FROM rental_items ri JOIN rentals r ON r.id=ri.rental_id WHERE ri.device_id=d.id AND r.status='renting')),0) AS busy,COALESCE(SUM(d.status='maintenance'),0) AS maintenance FROM devices d",null,null).get(0));
            List<Map<String,Object>> revenue=query(con,"SELECT DATE(rp.updated_at) AS day,SUM(rp.amount) AS revenue FROM rental_payments rp JOIN rentals r ON r.id=rp.rental_id WHERE rp.status='paid' AND r.status<>'cancelled' AND rp.updated_at>=? AND rp.updated_at<? GROUP BY DATE(rp.updated_at)",from,until);
            List<Map<String,Object>> orders=query(con,"SELECT DATE(created_at) AS day,COUNT(*) AS orders FROM rentals WHERE created_at>=? AND created_at<? GROUP BY DATE(created_at)",from,until);
            Map<String,Map<String,Object>> days=new LinkedHashMap<>();
            for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1)) {
                Map<String,Object> row=new HashMap<>(); row.put("day",day.toString());row.put("label",String.format("%02d/%02d",day.getDayOfMonth(),day.getMonthValue()));row.put("revenue",0);row.put("orders",0);days.put(day.toString(),row);
            }
            for(Map<String,Object> row:revenue) days.get(row.get("day").toString()).put("revenue",row.get("revenue"));
            for(Map<String,Object> row:orders) days.get(row.get("day").toString()).put("orders",row.get("orders"));
            double max=days.values().stream().mapToDouble(r->((Number)r.get("revenue")).doubleValue()).max().orElse(0);
            for(Map<String,Object> row:days.values()) row.put("height",max==0?0:((Number)row.get("revenue")).doubleValue()*100/max);
            data.put("days",new ArrayList<>(days.values()));
            data.put("top",query(con,"SELECT p.id,p.name,p.image,c.name AS category,COUNT(DISTINCT r.id) AS orders,COUNT(*) AS units,COALESCE(SUM(r.total*ri.price_per_day/NULLIF(w.rates,0)),0) AS value_total FROM rental_items ri JOIN rentals r ON r.id=ri.rental_id JOIN devices d ON d.id=ri.device_id JOIN products p ON p.id=d.product_id LEFT JOIN categories c ON c.id=p.id_category JOIN (SELECT rental_id,SUM(price_per_day) AS rates FROM rental_items GROUP BY rental_id) w ON w.rental_id=r.id WHERE r.status<>'cancelled' AND r.created_at>=? AND r.created_at<? GROUP BY p.id,p.name,p.image,c.name ORDER BY units DESC,p.id LIMIT 5",from,until));
            data.put("activity",query(con,"SELECT r.id,u.name,r.status,r.created_at FROM rentals r LEFT JOIN users u ON u.id=r.user_id WHERE r.created_at>=? AND r.created_at<? ORDER BY r.created_at DESC,r.id DESC LIMIT 8",from,until));
            return data;
        }
    }
}
