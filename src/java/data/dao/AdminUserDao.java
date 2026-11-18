package data.dao;

import data.driver.MySQLDriver;
import java.sql.*;
import java.util.*;
import model.User;
import service.Passwords;

public class AdminUserDao {
    protected Connection connection()throws SQLException {Connection con=MySQLDriver.getConnection();if(con==null)throw new SQLException("User database unavailable");return con;}
    public List<Map<String,Object>> findAll()throws SQLException {
        String sql="SELECT u.id,u.name,u.email,u.phone,u.role,u.status,u.created_at,COALESCE(r.orders,0) AS rental_count,COALESCE(p.revenue,0) AS revenue FROM users u LEFT JOIN (SELECT user_id,COUNT(*) AS orders FROM rentals GROUP BY user_id) r ON r.user_id=u.id LEFT JOIN (SELECT r.user_id,SUM(p.amount) AS revenue FROM rentals r JOIN rental_payments p ON p.rental_id=r.id WHERE p.status='paid' AND r.status<>'cancelled' GROUP BY r.user_id) p ON p.user_id=u.id ORDER BY u.id DESC";
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection con=connection();PreparedStatement st=con.prepareStatement(sql);ResultSet rs=st.executeQuery()) {
            while(rs.next()){Map<String,Object> row=new HashMap<>();for(String name:List.of("id","name","email","phone","role","status","created_at","rental_count","revenue"))row.put(name,rs.getObject(name));rows.add(row);}
        }
        return rows;
    }
    /** No password column is loaded into the session or management UI. */
    public User activePrincipal(int id)throws SQLException {
        try(Connection con=connection();PreparedStatement st=con.prepareStatement("SELECT id,name,email,phone,role,created_at FROM users WHERE id=? AND status='active'")) {
            st.setInt(1,id);try(ResultSet rs=st.executeQuery()){if(!rs.next())return null;return new User(rs.getInt("id"),rs.getString("name"),rs.getString("email"),rs.getString("phone"),null,rs.getString("role"),rs.getTimestamp("created_at"));}
        }
    }
    public static void validate(String name,String email,String phone,String role,String status) {
        if(name==null||name.isBlank()||name.length()>100||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Họ tên phải từ 1 đến 100 ký tự.");
        if(email==null||email.length()>150||!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"))throw new IllegalArgumentException("Email không hợp lệ hoặc quá 150 ký tự.");
        if(phone!=null&&!phone.isEmpty()&&!phone.matches("[0-9]{9,11}"))throw new IllegalArgumentException("Số điện thoại phải từ 9 đến 11 chữ số.");
        if(!Set.of("admin","user").contains(role==null?"":role))throw new IllegalArgumentException("Vai trò không hợp lệ.");
        if(!Set.of("active","locked").contains(status==null?"":status))throw new IllegalArgumentException("Trạng thái không hợp lệ.");
    }
    private void lockActor(Connection con,int actor)throws SQLException {
        boolean allowed=false;
        try(PreparedStatement st=con.prepareStatement("SELECT id,status FROM users WHERE role='admin' ORDER BY id FOR UPDATE");ResultSet rs=st.executeQuery()){while(rs.next())if(rs.getInt("id")==actor&&"active".equals(rs.getString("status")))allowed=true;}
        if(!allowed)throw new IllegalArgumentException("Tài khoản không còn quyền quản trị. Vui lòng đăng nhập lại.");
    }
    private void uniqueContact(Connection con,Integer id,String email,String phone)throws SQLException {
        try(PreparedStatement st=con.prepareStatement("SELECT id FROM users WHERE id<>? AND (email=? OR (?<>'' AND phone=?)) LIMIT 1")) {
            st.setInt(1,id==null?0:id);st.setString(2,email);st.setString(3,phone==null?"":phone);st.setString(4,phone);try(ResultSet rs=st.executeQuery()){if(rs.next())throw new IllegalArgumentException("Email hoặc số điện thoại đã được sử dụng.");}
        }
    }
    public void save(int actor,Integer id,String name,String email,String phone,String role,String status,String password)throws SQLException {
        validate(name,email,phone,role,status);if(actor<=0||id!=null&&id<=0)throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        if(id!=null&&id==actor&&(!"admin".equals(role)||!"active".equals(status)))throw new IllegalArgumentException("Không thể tự khóa hoặc hạ quyền tài khoản đang sử dụng.");
        String hashed=id==null?Passwords.hash(password):null;
        try(Connection con=connection()) {
            con.setAutoCommit(false);
            try {
                lockActor(con,actor);uniqueContact(con,id,email,phone);
                String sql=id==null?"INSERT INTO users(name,email,phone,role,status,password) VALUES(?,?,?,?,?,?)":"UPDATE users SET name=?,email=?,phone=?,role=?,status=? WHERE id=?";
                try(PreparedStatement st=con.prepareStatement(sql)){st.setString(1,name);st.setString(2,email);st.setString(3,phone==null||phone.isBlank()?null:phone);st.setString(4,role);st.setString(5,status);if(id==null)st.setString(6,hashed);else st.setInt(6,id);int changed=st.executeUpdate();if(changed!=1&&id==null)throw new SQLException("Account insert failed");if(changed!=1&&id!=null){try(PreparedStatement exists=con.prepareStatement("SELECT id FROM users WHERE id=?")){exists.setInt(1,id);try(ResultSet rs=exists.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Tài khoản không còn tồn tại.");}}}}
                con.commit();
            }catch(SQLException|RuntimeException ex){con.rollback();throw ex;}
        }
    }
    public void bulk(int actor,List<Integer> ids,String action)throws SQLException {
        if(ids==null||ids.isEmpty()||ids.size()>100||ids.stream().anyMatch(id->id==null||id<=0))throw new IllegalArgumentException("Chọn từ 1 đến 100 tài khoản hợp lệ.");
        if(!Set.of("lock","unlock","delete").contains(action==null?"":action))throw new IllegalArgumentException("Thao tác không hợp lệ.");
        if(ids.contains(actor)&&!"unlock".equals(action))throw new IllegalArgumentException("Không thể khóa hoặc xóa tài khoản đang sử dụng.");
        List<Integer> selected=ids.stream().distinct().sorted().toList();String slots=String.join(",",Collections.nCopies(selected.size(),"?"));
        try(Connection con=connection()) {
            con.setAutoCommit(false);
            try {
                lockActor(con,actor);
                try(PreparedStatement st=con.prepareStatement("SELECT id FROM users WHERE id IN ("+slots+") ORDER BY id FOR UPDATE")){bind(st,selected);int count=0;try(ResultSet rs=st.executeQuery()){while(rs.next())count++;}if(count!=selected.size())throw new IllegalArgumentException("Một số tài khoản không còn tồn tại. Vui lòng tải lại.");}
                if("delete".equals(action))try(PreparedStatement st=con.prepareStatement("SELECT 1 FROM rentals WHERE user_id IN ("+slots+") LIMIT 1")){bind(st,selected);try(ResultSet rs=st.executeQuery()){if(rs.next())throw new IllegalArgumentException("Không thể xóa tài khoản đã có đơn thuê. Có thể khóa tài khoản để giữ lịch sử.");}}
                String sql="delete".equals(action)?"DELETE FROM users WHERE id IN ("+slots+")":"UPDATE users SET status='"+("lock".equals(action)?"locked":"active")+"' WHERE id IN ("+slots+")";
                try(PreparedStatement st=con.prepareStatement(sql)){bind(st,selected);st.executeUpdate();}con.commit();
            }catch(SQLException|RuntimeException ex){con.rollback();throw ex;}
        }
    }
    private static void bind(PreparedStatement st,List<Integer> ids)throws SQLException{for(int i=0;i<ids.size();i++)st.setInt(i+1,ids.get(i));}
}
