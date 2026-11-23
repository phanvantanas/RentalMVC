package filter;

import data.dao.CheckoutDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;

/** Reclaim elapsed holds before stock, order and payment pages are processed. */
@WebFilter(
        filterName = "RentalHoldFilter",
        urlPatterns = {"/*"})
public class RentalHoldFilter implements Filter {

    protected CheckoutDao dao() {
        return new CheckoutDao();
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest q = (HttpServletRequest) req;
        String path = q.getServletPath();
        if (path != null
                && (path.isEmpty()
                        || path.equals("/")
                        || path.equals("/home")
                        || path.equals("/cart")
                        || path.equals("/checkout")
                        || path.equals("/my-rentals")
                        || path.equals("/payment/receipt")
                        || path.equals("/admin")
                        || path.startsWith("/admin/"))) {
            try {
                dao().expirePending();
            } catch (SQLException ex) {
                ((HttpServletResponse) res)
                        .sendError(503, "Không thể kiểm tra thời hạn giữ máy. Vui lòng thử lại.");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
