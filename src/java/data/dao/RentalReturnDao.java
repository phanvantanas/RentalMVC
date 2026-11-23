package data.dao;

import data.driver.MySQLDriver;

import service.LateFeePolicy;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public class RentalReturnDao {

    protected Connection connection() throws SQLException {
        Connection con = MySQLDriver.getConnection();
        if (con == null) {
            throw new SQLException("Return database unavailable");
        }
        return con;
    }

    private void admin(Connection con, int actor) throws SQLException {
        try (PreparedStatement st =
                con.prepareStatement(
                        "SELECT id FROM users WHERE id=? AND role='admin' AND status='active' FOR"
                            + " UPDATE")) {
            st.setInt(1, actor);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "Tài khoản không còn quyền xác nhận trả máy.");
                }
            }
        }
    }

    public void receive(
            int id,
            int actor,
            LocalDateTime actual,
            String legacyDueTime,
            BigDecimal adjusted,
            String reason,
            boolean collected)
            throws SQLException {
        if (id <= 0 || actor <= 0 || actual == null) {
            throw new IllegalArgumentException("Thông tin nhận máy không hợp lệ.");
        }
        LocalDateTime now = LocalDateTime.now(LateFeePolicy.ZONE);
        if (actual.isAfter(now)) {
            throw new IllegalArgumentException("Giờ trả thực tế không được nằm trong tương lai.");
        }
        reason = reason == null ? "" : reason.trim();
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Lý do giảm phí tối đa 500 ký tự.");
        }
        try (Connection con = connection()) {
            con.setAutoCommit(false);
            try {
                admin(con, actor);
                LocalDate start, end;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT status,start_date,end_date FROM rentals WHERE id=? FOR"
                                    + " UPDATE")) {
                    st.setInt(1, id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next() || !"renting".equals(rs.getString("status"))) {
                            throw new IllegalArgumentException(
                                    "Đơn không còn ở trạng thái đang thuê. Vui lòng tải lại.");
                        }
                        if (rs.getDate("start_date") == null || rs.getDate("end_date") == null) {
                            throw new IllegalArgumentException("Đơn chưa có lịch thuê hợp lệ.");
                        }
                        start = rs.getDate("start_date").toLocalDate();
                        end = rs.getDate("end_date").toLocalDate();
                    }
                }
                if (end.isBefore(start) || actual.isBefore(start.atStartOfDay())) {
                    throw new IllegalArgumentException(
                            "Giờ trả thực tế phải từ ngày bắt đầu thuê trở đi.");
                }
                LocalTime time = null;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT return_time FROM rental_payments WHERE rental_id=? FOR"
                                    + " UPDATE")) {
                    st.setInt(1, id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next() && rs.getTime(1) != null) {
                            time = rs.getTime(1).toLocalTime();
                        }
                    }
                }
                if (time == null) {
                    try {
                        time = LocalTime.parse(Objects.toString(legacyDueTime, ""));
                    } catch (RuntimeException ex) {
                        throw new IllegalArgumentException(
                                "Vui lòng xác nhận giờ hẹn trả cho đơn cũ.");
                    }
                }
                BigDecimal rate;
                int count;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "SELECT COUNT(*) AS items,COALESCE(SUM(price_per_day),0) AS"
                                    + " rate,COALESCE(SUM(price_per_day<0 OR price_per_day IS"
                                    + " NULL),0) AS invalid FROM rental_items WHERE rental_id=?")) {
                    st.setInt(1, id);
                    try (ResultSet rs = st.executeQuery()) {
                        rs.next();
                        count = rs.getInt("items");
                        rate = rs.getBigDecimal("rate");
                        if (count == 0 || rs.getInt("invalid") > 0) {
                            throw new IllegalArgumentException(
                                    "Đơn chưa có giá thuê thiết bị hợp lệ để tính phí.");
                        }
                    }
                }
                LocalDateTime due = end.atTime(time);
                var quote = LateFeePolicy.calculate(due, actual, rate);
                BigDecimal fee = adjusted == null ? quote.fee() : adjusted;
                if (fee.signum() < 0 || fee.scale() > 2 || fee.compareTo(quote.fee()) > 0) {
                    throw new IllegalArgumentException(
                            "Phí thực thu phải từ 0 đến mức phí dự kiến.");
                }
                if (fee.compareTo(quote.fee()) < 0 && reason.isBlank()) {
                    throw new IllegalArgumentException(
                            "Vui lòng nhập lý do giảm hoặc miễn phí trả trễ.");
                }
                String status = fee.signum() == 0 ? "waived" : collected ? "paid" : "unpaid";
                try (PreparedStatement st =
                        con.prepareStatement(
                                "INSERT INTO"
                                    + " rental_returns(rental_id,due_at,actual_return_at,late_seconds,daily_rate,charged_percent,calculated_fee,final_fee,adjustment_reason,received_by,fee_status,fee_paid_at,fee_received_by,policy_version)"
                                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    st.setInt(1, id);
                    st.setObject(2, due);
                    st.setObject(3, actual);
                    st.setLong(4, quote.seconds());
                    st.setBigDecimal(5, rate);
                    st.setInt(6, quote.percent());
                    st.setBigDecimal(7, quote.fee());
                    st.setBigDecimal(8, fee);
                    st.setString(9, reason.isBlank() ? null : reason);
                    st.setInt(10, actor);
                    st.setString(11, status);
                    st.setObject(12, "paid".equals(status) ? now : null);
                    if ("paid".equals(status)) {
                        st.setInt(13, actor);
                    } else {
                        st.setNull(13, Types.INTEGER);
                    }
                    st.setString(14, LateFeePolicy.VERSION);
                    st.executeUpdate();
                }
                try (PreparedStatement st =
                        con.prepareStatement(
                                "UPDATE rentals SET status='returned',late_fee=? WHERE id=? AND"
                                    + " status='renting'")) {
                    st.setBigDecimal(1, fee);
                    st.setInt(2, id);
                    if (st.executeUpdate() != 1) {
                        throw new SQLException("Return state changed");
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public boolean confirmFee(int id, int actor) throws SQLException {
        try (Connection con = connection()) {
            con.setAutoCommit(false);
            try {
                admin(con, actor);
                try (PreparedStatement st =
                        con.prepareStatement("SELECT status FROM rentals WHERE id=? FOR UPDATE")) {
                    st.setInt(1, id);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next() || !"returned".equals(rs.getString(1))) {
                            throw new IllegalArgumentException(
                                    "Chỉ xác nhận phí cho đơn đã trả máy.");
                        }
                    }
                }
                int updated;
                try (PreparedStatement st =
                        con.prepareStatement(
                                "UPDATE rental_returns SET"
                                    + " fee_status='paid',fee_paid_at=?,fee_received_by=? WHERE"
                                    + " rental_id=? AND fee_status='unpaid' AND final_fee>0")) {
                    st.setObject(1, LocalDateTime.now(LateFeePolicy.ZONE));
                    st.setInt(2, actor);
                    st.setInt(3, id);
                    updated = st.executeUpdate();
                }
                con.commit();
                return updated == 1;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public Map<Integer, Map<String, Object>> findAllDetails() throws SQLException {
        Map<Integer, Map<String, Object>> rows = new HashMap<>();
        try (Connection con = connection();
                PreparedStatement st =
                        con.prepareStatement(
                                "SELECT rr.*,u.name AS receiver_name FROM rental_returns rr LEFT"
                                    + " JOIN users u ON u.id=rr.received_by");
                ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (String key :
                        List.of(
                                "due_at",
                                "actual_return_at",
                                "late_seconds",
                                "daily_rate",
                                "charged_percent",
                                "calculated_fee",
                                "final_fee",
                                "adjustment_reason",
                                "fee_status",
                                "fee_paid_at",
                                "received_at",
                                "receiver_name",
                                "policy_version")) {
                    row.put(key, rs.getObject(key));
                }
                rows.put(rs.getInt("rental_id"), row);
            }
        }
        return rows;
    }
}
