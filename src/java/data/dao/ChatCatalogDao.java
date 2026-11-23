package data.dao;

import data.driver.MySQLDriver;

import java.sql.*;

/** Public product information only; no customer, order or payment data is sent to AI. */
public class ChatCatalogDao {
    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) throw new SQLException("Chat catalog unavailable");
        return con;
    }

    public String context() throws SQLException {
        String sql =
                "SELECT p.id,p.name,p.price,p.description,c.name AS category FROM products p LEFT"
                    + " JOIN categories c ON c.id=p.id_category WHERE p.status='active' ORDER BY"
                    + " p.id DESC LIMIT 40";
        StringBuilder out = new StringBuilder();
        try (Connection con = connection();
                PreparedStatement st = con.prepareStatement(sql);
                ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                String description = rs.getString("description");
                if (description == null) description = "";
                if (description.length() > 350) description = description.substring(0, 350);
                out.append("ID ")
                        .append(rs.getInt("id"))
                        .append(" | ")
                        .append(rs.getString("name"))
                        .append(" | ")
                        .append(rs.getString("category"))
                        .append(" | ")
                        .append(rs.getBigDecimal("price").toPlainString())
                        .append(" VND/ngày | ")
                        .append(description.replace('\n', ' '))
                        .append('\n');
            }
        }
        return out.isEmpty()
                ? "Hiện không có dữ liệu sản phẩm để xác nhận giá. Hãy hướng dẫn khách xem danh mục"
                      + " trên website."
                : out.toString();
    }
}
