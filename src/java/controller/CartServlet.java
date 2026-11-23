package controller;

import data.dao.Database;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.*;

import service.CartSupport;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet(
        name = "CartServlet",
        urlPatterns = {"/cart"})
public class CartServlet extends HttpServlet {

    protected Product product(int id) {
        return Database.getProductDao().find(id);
    }

    protected int available(int id, LocalDate start, LocalDate end) {
        return Database.getDeviceDao().countAvailable(start, end).getOrDefault(id, 0);
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        s.setHeader("Cache-Control", "no-store");
        synchronized (q.getSession()) {
            CartSupport.prepare(q);
            q.setAttribute("cartError", q.getSession().getAttribute("cartError"));
            q.getSession().removeAttribute("cartError");
            q.getRequestDispatcher("/WEB-INF/views/shop/cart.jsp").forward(q, s);
        }
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        q.setCharacterEncoding("UTF-8");
        if (!CartSupport.validToken(q)) {
            s.sendError(403);
            return;
        }
        String action = q.getParameter("action");
        String destination = "/cart";
        synchronized (q.getSession()) {
            RentalCart cart = CartSupport.cart(q.getSession());
            try {
                if ("clear".equals(action)) {
                    cart.clear();
                } else if ("remove".equals(action)) {
                    cart.remove(Integer.parseInt(q.getParameter("product")));
                } else if ("update".equals(action)) {
                    cart.update(
                            Integer.parseInt(q.getParameter("product")),
                            Integer.parseInt(q.getParameter("quantity")));
                } else if ("add".equals(action) || "buy".equals(action)) {
                    boolean direct = "buy".equals(action);
                    if (direct) {
                        Object previous = q.getSession().getAttribute("directRentalCart");
                        if (previous instanceof RentalCart old && old.getPendingOrderId() > 0) {
                            throw new IllegalArgumentException(
                                    "Bạn còn đơn thanh toán ngay #ORD-"
                                            + old.getPendingOrderId()
                                            + " chưa hoàn tất. Hãy kiểm tra trong Đơn thuê của tôi"
                                            + " hoặc chờ hết hạn giữ máy trước khi tạo đơn mới.");
                        }
                        cart = new RentalCart();
                    }
                    int id = Integer.parseInt(q.getParameter("product"));
                    LocalDate start = LocalDate.parse(q.getParameter("start")),
                            end = LocalDate.parse(q.getParameter("end"));
                    RentalCart.validateDates(start, end);
                    Product product = product(id);
                    if (product == null || !"active".equals(product.getStatus())) {
                        throw new IllegalArgumentException("Thiết bị không còn cho thuê.");
                    }
                    int required = cart.getQuantities().getOrDefault(id, 0) + 1;
                    int stock = available(id, start, end);
                    if (q.getSession().getAttribute("user") instanceof User user
                            && cart.getPendingUserId() == user.getId()
                            && start.equals(cart.getStart())
                            && end.equals(cart.getEnd())) {
                        stock += cart.getPendingQuantities().getOrDefault(id, 0);
                    }
                    if (stock < required) {
                        throw new IllegalArgumentException(
                                "Không đủ thiết bị trống cho lịch thuê đã chọn.");
                    }
                    cart.add(
                            id,
                            1,
                            start,
                            end,
                            q.getParameter("pickup_time"),
                            q.getParameter("return_time"));
                    if (direct) {
                        q.getSession().setAttribute("directRentalCart", cart);
                        destination = "/checkout?direct=1";
                    }
                } else {
                    throw new IllegalArgumentException("Thao tác giỏ hàng không hợp lệ.");
                }
                String suffix = "buy".equals(action) ? "Direct" : "";
                q.getSession().removeAttribute("checkoutNonce" + suffix);
                q.getSession().removeAttribute("checkoutQuote" + suffix);
            } catch (RuntimeException ex) {
                q.getSession()
                        .setAttribute(
                                "cartError",
                                ex instanceof IllegalArgumentException
                                                && !(ex instanceof NumberFormatException)
                                        ? ex.getMessage()
                                        : "Thông tin giỏ hàng không hợp lệ.");
                destination = "/cart";
            }
        }
        s.sendRedirect(q.getContextPath() + destination);
    }
}
