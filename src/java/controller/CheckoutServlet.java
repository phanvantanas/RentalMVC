package controller;

import data.dao.CheckoutDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.*;

import service.*;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(
        name = "CheckoutServlet",
        urlPatterns = {"/checkout"})
public class CheckoutServlet extends HttpServlet {

    protected CheckoutDao dao() {
        return new CheckoutDao();
    }

    protected MerchantBank bank() throws IOException {
        return MerchantBank.load(getServletContext());
    }

    private User user(HttpServletRequest q) {
        Object user = q.getSession().getAttribute("user");
        return user instanceof User ? (User) user : null;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        s.setHeader("Cache-Control", "no-store");
        User user = user(q);
        if (user == null) {
            String order = q.getParameter("order");
            q.getSession()
                    .setAttribute(
                            "afterLogin",
                            order != null && order.matches("[0-9]+")
                                    ? "/checkout?order=" + order
                                    : "/checkout"
                                            + (CartSupport.directCheckout(q) ? "?direct=1" : ""));
            s.sendRedirect(q.getContextPath() + "/login");
            return;
        }
        synchronized (q.getSession()) {
            CartSupport.token(q.getSession());
            String id = q.getParameter("order");
            if (id != null) {
                try {
                    var payment = dao().payment(Integer.parseInt(id), user.getId());
                    if (payment == null) {
                        s.sendError(404);
                        return;
                    }
                    q.setAttribute("payment", payment);
                } catch (NumberFormatException ex) {
                    s.sendError(400);
                    return;
                } catch (SQLException ex) {
                    throw new ServletException("Unable to load payment", ex);
                }
            } else {
                RentalCart cart = CartSupport.checkoutCart(q);
                if (cart == null) {
                    s.sendRedirect(q.getContextPath() + "/home");
                    return;
                }
                if (cart.getPendingOrderId() > 0) {
                    if (cart.getPendingUserId() != user.getId()) {
                        s.sendError(409);
                        return;
                    }
                    s.sendRedirect(
                            q.getContextPath() + "/checkout?order=" + cart.getPendingOrderId());
                    return;
                }
                if (cart.getCount() == 0) {
                    s.sendRedirect(q.getContextPath() + "/cart");
                    return;
                }
                CartSupport.prepare(q, cart);
                q.setAttribute("directCheckout", CartSupport.directCheckout(q));
                q.getSession()
                        .setAttribute(
                                CartSupport.checkoutKey(q, "checkoutQuote"),
                                q.getAttribute("cartTotal"));
                q.setAttribute("bankReady", bank().configured());
                q.getSession()
                        .setAttribute(
                                CartSupport.checkoutKey(q, "checkoutNonce"),
                                CartSupport.newToken());
                q.setAttribute(
                        "checkoutNonce",
                        q.getSession().getAttribute(CartSupport.checkoutKey(q, "checkoutNonce")));
            }
            q.setAttribute("checkoutError", q.getSession().getAttribute("checkoutError"));
            q.getSession().removeAttribute("checkoutError");
            q.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp").forward(q, s);
        }
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        q.setCharacterEncoding("UTF-8");
        User user = user(q);
        if (user == null) {
            s.sendError(401);
            return;
        }
        if (!CartSupport.validToken(q)) {
            s.sendError(403);
            return;
        }
        synchronized (q.getSession()) {
            try {
                if ("reported".equals(q.getParameter("action"))) {
                    int id = Integer.parseInt(q.getParameter("order"));
                    CheckoutDao dao = dao();
                    if ("missing".equals(dao.cartPaymentState(id, user.getId()))) {
                        s.sendError(404);
                        return;
                    }
                    dao.report(id, user.getId());
                    s.sendRedirect(q.getContextPath() + "/checkout?order=" + id);
                    return;
                }
                RentalCart cart = CartSupport.checkoutCart(q);
                if (cart == null) {
                    s.sendError(409, "Phiên thanh toán đã thay đổi. Vui lòng chọn lại thiết bị.");
                    return;
                }
                if (cart.getPendingOrderId() > 0) {
                    if (cart.getPendingUserId() != user.getId()) {
                        s.sendError(409);
                        return;
                    }
                    s.sendRedirect(
                            q.getContextPath() + "/checkout?order=" + cart.getPendingOrderId());
                    return;
                }
                String nonce = q.getParameter("nonce");
                if (nonce == null
                        || !nonce.equals(
                                q.getSession()
                                        .getAttribute(
                                                CartSupport.checkoutKey(q, "checkoutNonce")))) {
                    s.sendError(
                            409,
                            "Đơn đang được xử lý hoặc đã được gửi. Vui lòng kiểm tra lịch thuê.");
                    return;
                }
                Object quote =
                        q.getSession().getAttribute(CartSupport.checkoutKey(q, "checkoutQuote"));
                if (!(quote instanceof java.math.BigDecimal)) {
                    s.sendError(409);
                    return;
                }
                int id = dao().place(user.getId(), cart, bank(), (java.math.BigDecimal) quote);
                cart.beginPayment(id, user.getId());
                q.getSession().removeAttribute(CartSupport.checkoutKey(q, "checkoutNonce"));
                q.getSession().removeAttribute(CartSupport.checkoutKey(q, "checkoutQuote"));
                s.sendRedirect(q.getContextPath() + "/checkout?order=" + id);
            } catch (IllegalArgumentException ex) {
                q.getSession()
                        .setAttribute(
                                "checkoutError",
                                ex instanceof NumberFormatException
                                        ? "Thông tin đơn thuê không hợp lệ."
                                        : ex.getMessage());
                s.sendRedirect(
                        q.getContextPath()
                                + "/checkout"
                                + (CartSupport.directCheckout(q) ? "?direct=1" : ""));
            } catch (SQLException ex) {
                log("Checkout failed", ex);
                q.getSession()
                        .setAttribute(
                                "checkoutError",
                                "Không thể tạo đơn thuê. Vui lòng kiểm tra lại và thử lần nữa.");
                s.sendRedirect(
                        q.getContextPath()
                                + "/checkout"
                                + (CartSupport.directCheckout(q) ? "?direct=1" : ""));
            }
        }
    }
}
