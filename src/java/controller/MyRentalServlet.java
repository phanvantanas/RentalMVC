package controller;

import data.dao.MyRentalDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.RentalCart;
import model.User;

import service.CartSupport;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet(
        name = "MyRentalServlet",
        urlPatterns = {"/my-rentals"})
public class MyRentalServlet extends HttpServlet {

    protected MyRentalDao dao() {
        return new MyRentalDao();
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        s.setHeader("Cache-Control", "no-store");
        Integer id = null;
        try {
            if (q.getParameter("order") != null) {
                id = Integer.valueOf(q.getParameter("order"));
                if (id <= 0) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException ex) {
            s.sendError(400);
            return;
        }
        if (!(q.getSession().getAttribute("user") instanceof User user)) {
            q.getSession()
                    .setAttribute("afterLogin", "/my-rentals" + (id == null ? "" : "?order=" + id));
            s.sendRedirect(q.getContextPath() + "/login");
            return;
        }
        CartSupport.token(q.getSession());
        for (String key : List.of("rentalFlash", "rentalFlashError")) {
            q.setAttribute(key, q.getSession().getAttribute(key));
            q.getSession().removeAttribute(key);
        }
        try {
            var orders = dao().findOwned(user.getId(), id);
            if (id != null && orders.isEmpty()) {
                s.sendError(404);
                return;
            }
            q.setAttribute("myOrders", orders);
            q.setAttribute("detailOrder", id != null);
        } catch (SQLException ex) {
            log("Cannot load customer rentals", ex);
            s.setStatus(503);
            q.setAttribute("loadError", "Chưa tải được đơn thuê. Vui lòng thử lại sau.");
        }
        q.setAttribute(
                "rentalLabels",
                Map.of(
                        "pending",
                        "Chờ duyệt",
                        "confirmed",
                        "Chờ nhận máy",
                        "renting",
                        "Đang thuê",
                        "returned",
                        "Đã trả máy",
                        "cancelled",
                        "Đã hủy"));
        q.setAttribute(
                "paymentLabels",
                Map.of(
                        "pending",
                        "Chờ chuyển khoản",
                        "reported",
                        "Chờ đối soát",
                        "paid",
                        "Đã nhận tiền"));
        q.getRequestDispatcher("/WEB-INF/views/shop/my-rentals.jsp").forward(q, s);
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        q.setCharacterEncoding("UTF-8");
        s.setHeader("Cache-Control", "no-store");
        HttpSession session = q.getSession();
        if (!(session.getAttribute("user") instanceof User user)) {
            s.sendError(401);
            return;
        }
        if (!CartSupport.validToken(q)) {
            s.sendError(403);
            return;
        }
        if (!"cancel".equals(q.getParameter("action"))) {
            s.sendError(400);
            return;
        }
        int order;
        try {
            order = Integer.parseInt(q.getParameter("order"));
            if (order <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            s.sendError(400);
            return;
        }
        synchronized (session) {
            try {
                if (dao().cancelUnpaid(order, user.getId())) {
                    for (String key : List.of("rentalCart", "directRentalCart")) {
                        if (session.getAttribute(key) instanceof RentalCart cart
                                && cart.getPendingOrderId() == order
                                && cart.getPendingUserId() == user.getId()) {
                            cart.finishPayment(order, user.getId(), false);
                            String suffix = "directRentalCart".equals(key) ? "Direct" : "";
                            session.removeAttribute("checkoutNonce" + suffix);
                            session.removeAttribute("checkoutQuote" + suffix);
                        }
                    }
                    session.setAttribute(
                            "rentalFlash",
                            "Đã hủy đơn #ORD-"
                                    + order
                                    + " và giải phóng thiết bị. Vui lòng không chuyển khoản cho đơn"
                                    + " này.");
                } else
                    session.setAttribute(
                            "rentalFlashError",
                            "Không thể hủy đơn. Chỉ được hủy đơn của bạn chưa báo chuyển khoản,"
                                + " chưa gửi biên lai và chưa nhận máy. Vui lòng tải lại trạng"
                                + " thái; nếu đã chuyển tiền, hãy liên hệ LensCam.");
            } catch (SQLException ex) {
                log("Customer cancellation failed", ex);
                session.setAttribute(
                        "rentalFlashError", "Không thể hủy đơn lúc này. Vui lòng thử lại.");
            }
        }
        s.sendRedirect(q.getContextPath() + "/my-rentals?order=" + order);
    }
}
