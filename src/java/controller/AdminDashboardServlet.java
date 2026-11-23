package controller;

import data.dao.DashboardDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@WebServlet(
        name = "AdminDashboardServlet",
        urlPatterns = {"/admin", "/admin/overview"})
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                from = today.withDayOfMonth(1),
                to = today;
        String period = Optional.ofNullable(request.getParameter("period")).orElse("month");
        try {
            switch (period) {
                case "today":
                    from = today;
                    break;
                case "week":
                    from = today.minusDays(6);
                    break;
                case "custom":
                    from = LocalDate.parse(request.getParameter("from"));
                    to = LocalDate.parse(request.getParameter("to"));
                    break;
                case "month":
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            if (to.isBefore(from)
                    || ChronoUnit.DAYS.between(from, to) > 365
                    || from.getYear() < 2000
                    || to.getYear() > 2100) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            response.sendError(400, "Khoảng ngày không hợp lệ. Chọn tối đa 366 ngày.");
            return;
        }
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("period", period);
        request.setAttribute("adminSection", "overview");
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
        try {
            Map<String, Object> dashboard = new DashboardDao().load(from, to);
            Set<String> images = getServletContext().getResourcePaths("/assets/images/");
            for (Object item : (List<?>) dashboard.get("top")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) item;
                String image = Objects.toString(row.get("image"), "").replace('\\', '/');
                String name = image.substring(image.lastIndexOf('/') + 1);
                if (images != null && !name.isEmpty()) {
                    for (String file : images) {
                        String actual = file.substring(file.lastIndexOf('/') + 1);
                        if (actual.equalsIgnoreCase(name)) {
                            row.put(
                                    "imageUrl",
                                    request.getContextPath()
                                            + "/assets/images/"
                                            + URLEncoder.encode(actual, StandardCharsets.UTF_8)
                                                    .replace("+", "%20"));
                            break;
                        }
                    }
                }
            }
            request.setAttribute("dashboard", dashboard);
            if ("csv".equals(request.getParameter("export"))) {
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader(
                        "Content-Disposition", "attachment; filename=\"lenscam-overview.csv\"");
                var out = response.getWriter();
                out.write('\uFEFF');
                out.println("Ngày,Tiền thuê đã xác nhận (VND),Đơn được tạo");
                for (Object item : (List<?>) dashboard.get("days")) {
                    Map<?, ?> row = (Map<?, ?>) item;
                    out.println(
                            row.get("day") + "," + row.get("revenue") + "," + row.get("orders"));
                }
                return;
            }
        } catch (SQLException ex) {
            log("Cannot load dashboard", ex);
            response.setStatus(503);
            request.setAttribute(
                    "loadError", "Không thể tải số liệu từ database. Vui lòng thử lại.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/overview.jsp")
                .forward(request, response);
    }
}
