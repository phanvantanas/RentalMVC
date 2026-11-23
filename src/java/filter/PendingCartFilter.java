package filter;

import data.dao.CheckoutDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import model.RentalCart;
import model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

/** Reconcile only the current customer's cart; a reported transfer is not a paid order. */
@WebFilter(
        filterName = "PendingCartFilter",
        urlPatterns = {"/*"})
public class PendingCartFilter implements Filter {

    protected CheckoutDao dao() {
        return new CheckoutDao();
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest q = (HttpServletRequest) req;
        HttpServletResponse s = (HttpServletResponse) res;
        if (Set.of("", "/", "/home", "/cart", "/checkout", "/my-rentals")
                .contains(q.getServletPath())) {
            HttpSession session = q.getSession(false);
            if (session != null)
                synchronized (session) {
                    for (String key : new String[] {"rentalCart", "directRentalCart"}) {
                        Object principal = session.getAttribute("user"),
                                value = session.getAttribute(key);
                        if (principal instanceof User user
                                && value instanceof RentalCart cart
                                && cart.getPendingOrderId() > 0
                                && cart.getPendingUserId() == user.getId()) {
                            try {
                                String status =
                                        dao().cartPaymentState(
                                                        cart.getPendingOrderId(), user.getId());
                                if ("paid".equals(status)
                                        || "cancelled".equals(status)
                                        || "missing".equals(status)) {
                                    cart.finishPayment(
                                            cart.getPendingOrderId(),
                                            user.getId(),
                                            "paid".equals(status));
                                    String suffix = "directRentalCart".equals(key) ? "Direct" : "";
                                    session.removeAttribute("checkoutNonce" + suffix);
                                    session.removeAttribute("checkoutQuote" + suffix);
                                }
                            } catch (SQLException ex) {
                                throw new ServletException(
                                        "Không thể kiểm tra trạng thái thanh toán. Vui lòng thử"
                                            + " lại.",
                                        ex);
                            }
                        }
                    }
                }
        }
        chain.doFilter(req, res);
    }
}
