package controller;

import data.dao.AdminRentalDao;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.AdminRental;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;

@WebServlet(
        name = "AdminRentalServlet",
        urlPatterns = {"/admin/rentals"})
public class AdminRentalServlet extends HttpServlet {
    protected data.dao.RentalReturnDao returnDao() {
        return new data.dao.RentalReturnDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute("adminCsrf") == null) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                session.setAttribute("adminCsrf", HexFormat.of().formatHex(bytes));
            }
        }
        try {
            List<AdminRental> rentals = new AdminRentalDao().findAll();
            Map<String, Integer> counts = new HashMap<>();
            for (AdminRental row : rentals) {
                counts.merge(row.getStatus(), 1, Integer::sum);
                if (row.isOverdue()) counts.merge("overdue", 1, Integer::sum);
            }
            request.setAttribute("rentals", rentals);
            request.setAttribute("counts", counts);
            request.setAttribute("returnDetails", returnDao().findAllDetails());
        } catch (SQLException ex) {
            log("Unable to load admin rentals", ex);
            request.setAttribute(
                    "loadError", "Không thể tải đơn thuê từ database. Vui lòng thử lại.");
            response.setStatus(503);
        }
        request.setAttribute(
                "statusLabels",
                Map.of(
                        "pending",
                        "Chờ duyệt",
                        "confirmed",
                        "Chờ giao máy",
                        "renting",
                        "Đang thuê",
                        "returned",
                        "Đã trả",
                        "cancelled",
                        "Đã hủy"));
        request.setAttribute("flash", session.getAttribute("adminFlash"));
        request.setAttribute("flashError", session.getAttribute("adminFlashError"));
        session.removeAttribute("adminFlash");
        session.removeAttribute("adminFlashError");
        request.getRequestDispatcher("/WEB-INF/views/admin/rentals.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String token = request.getParameter("csrf");
        Object expectedToken = request.getSession().getAttribute("adminCsrf");
        if (!(expectedToken instanceof String)
                || token == null
                || !MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        ((String) expectedToken).getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            return;
        }
        String expected = request.getParameter("expected"), next = request.getParameter("status");
        String action = request.getParameter("action");
        if ("return_rental".equals(action) || "confirm_late_fee".equals(action)) {
            if (!(request.getSession().getAttribute("user") instanceof model.User actor)
                    || !"admin".equals(actor.getRole())) {
                response.sendError(403);
                return;
            }
            Integer id = null;
            try {
                id = Integer.valueOf(request.getParameter("id"));
                if ("return_rental".equals(action)) {
                    String adjusted = request.getParameter("adjusted_fee");
                    returnDao()
                            .receive(
                                    id,
                                    actor.getId(),
                                    java.time.LocalDateTime.parse(
                                            Objects.toString(
                                                    request.getParameter("actual_return"), "")),
                                    request.getParameter("legacy_due_time"),
                                    adjusted == null || adjusted.isBlank()
                                            ? null
                                            : new java.math.BigDecimal(adjusted),
                                    request.getParameter("adjustment_reason"),
                                    "1".equals(request.getParameter("fee_collected")));
                    request.getSession()
                            .setAttribute(
                                    "adminFlash",
                                    "Đã nhận lại máy và lưu phí trả trễ cho đơn #ORD-" + id + ".");
                } else {
                    boolean updated = returnDao().confirmFee(id, actor.getId());
                    request.getSession()
                            .setAttribute(
                                    updated ? "adminFlash" : "adminFlashError",
                                    updated
                                            ? "Đã xác nhận thu phí trả trễ cho đơn #ORD-" + id + "."
                                            : "Phí đã được xác nhận hoặc đơn không có phí cần"
                                                  + " thu.");
                }
            } catch (java.time.DateTimeException ex) {
                request.getSession()
                        .setAttribute("adminFlashError", "Giờ trả thực tế không hợp lệ.");
            } catch (IllegalArgumentException ex) {
                request.getSession()
                        .setAttribute(
                                "adminFlashError",
                                ex instanceof NumberFormatException
                                        ? "Thông tin phí hoặc mã đơn không hợp lệ."
                                        : ex.getMessage());
            } catch (SQLException ex) {
                log("Rental return failed", ex);
                request.getSession()
                        .setAttribute(
                                "adminFlashError",
                                "Không thể lưu trả máy/phí trễ. Vui lòng tải lại và thử lại.");
            }
            response.sendRedirect(
                    request.getContextPath()
                            + "/admin/rentals"
                            + (id != null && id > 0 ? "?order=" + id : ""));
            return;
        }
        if ("confirm_payment".equals(request.getParameter("action"))) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                boolean updated = new data.dao.CheckoutDao().confirmPaid(id);
                request.getSession()
                        .setAttribute(
                                updated ? "adminFlash" : "adminFlashError",
                                updated
                                        ? "Đã xác nhận nhận tiền cho đơn #ORD-" + id + "."
                                        : "Đơn đã được đối soát hoặc không còn hợp lệ.");
            } catch (NumberFormatException ex) {
                response.sendError(400);
                return;
            } catch (SQLException ex) {
                log("Payment confirmation failed", ex);
                request.getSession()
                        .setAttribute("adminFlashError", "Không thể cập nhật thanh toán.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/rentals");
            return;
        }
        if (expected == null || next == null) {
            response.sendError(400);
            return;
        }
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            if (id <= 0) throw new NumberFormatException();
            String error = new AdminRentalDao().transition(id, expected, next);
            request.getSession()
                    .setAttribute(
                            error == null ? "adminFlash" : "adminFlashError",
                            error == null ? "Đã cập nhật trạng thái đơn #ORD-" + id + "." : error);
        } catch (NumberFormatException ex) {
            response.sendError(400);
            return;
        } catch (SQLException ex) {
            log("Unable to update rental", ex);
            request.getSession()
                    .setAttribute("adminFlashError", "Cập nhật thất bại. Vui lòng thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/rentals");
    }
}
