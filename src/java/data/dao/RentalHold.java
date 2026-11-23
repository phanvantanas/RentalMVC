package data.dao;

import java.sql.*;

/** Database time is authoritative; reported transfers await manual reconciliation. */
public final class RentalHold {
    private RentalHold() {}

    public static final int MINUTES = 30;
    public static final String DEADLINE = "p.created_at + INTERVAL " + MINUTES + " MINUTE";
    public static final String EXPIRED =
            "p.status='pending' AND p.created_at<=CURRENT_TIMESTAMP - INTERVAL "
                    + MINUTES
                    + " MINUTE";

    public static int expire(Connection con) throws SQLException {
        return expire(con, null);
    }

    public static int expire(Connection con, Integer rentalId) throws SQLException {
        String sql =
                "UPDATE rentals r JOIN rental_payments p ON p.rental_id=r.id SET"
                    + " r.status='cancelled' WHERE r.status IN ('pending','confirmed') AND "
                        + EXPIRED;
        if (rentalId != null) sql += " AND r.id=?";
        try (PreparedStatement st = con.prepareStatement(sql)) {
            if (rentalId != null) st.setInt(1, rentalId);
            return st.executeUpdate();
        }
    }
}
