package controller;

import data.dao.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.Category;
import model.Product;
import model.User;

@WebServlet(name = "HomeServlet", urlPatterns = {"/home", ""})
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate start = today;
        LocalDate end = today.plusDays(3);
        try {
            if (request.getParameter("start") != null) start = LocalDate.parse(request.getParameter("start"));
            if (request.getParameter("end") != null) end = LocalDate.parse(request.getParameter("end"));
            if (start.isBefore(today) || !end.isAfter(start)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException ex) {
            start = today;
            end = today.plusDays(3);
            request.setAttribute("dateError", "Vui lòng chọn ngày nhận từ hôm nay và ngày trả sau ngày nhận.");
        }
        List<Category> categories = Database.getCategoryDao().findAll();
        List<Product> products = Database.getProductDao().findAll();
        products.removeIf(p -> !"active".equalsIgnoreCase(p.getStatus()));
        Map<Integer, String> categoryNames = new HashMap<>();
        Map<Integer, Integer> categoryCounts = new HashMap<>();
        for (Category category : categories) categoryNames.put(category.getId(), category.getName());
        Map<Integer, String> images = new HashMap<>();
        Set<String> resources = getServletContext().getResourcePaths("/assets/images/");
        for (Product product : products) {
            categoryCounts.merge(product.getId_category(), 1, Integer::sum);
            String match = findImage(product, resources);
            if (match != null) images.put(product.getId(), request.getContextPath() + "/assets/images/"
                    + URLEncoder.encode(match, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        request.setAttribute("lstCategory", categories);
        service.CartSupport.token(request.getSession());
        request.setAttribute("cartCount", service.CartSupport.cart(request.getSession()).getCount());
        request.setAttribute("lstProduct", products);
        request.setAttribute("categoryNames", categoryNames);
        request.setAttribute("categoryCounts", categoryCounts);
        request.setAttribute("productImages", images);
        request.setAttribute("availableCounts", Database.getDeviceDao().countAvailable(start, end));
        request.setAttribute("startDate", start.toString());
        request.setAttribute("endDate", end.toString());
        request.setAttribute("today", today.toString());
        request.setAttribute("rentalDays", ChronoUnit.DAYS.between(start, end));
        request.setAttribute("pickupTime", validTime(request.getParameter("pickup_time"), "08:00"));
        request.setAttribute("returnTime", validTime(request.getParameter("return_time"), "17:00"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM");
        request.setAttribute("dateLabel", start.format(format) + " – " + end.format(format));
        User user = (User) request.getSession().getAttribute("user");
        if (user != null) request.setAttribute("myRentals", Database.getRentalDao().findByUserId(user.getId()));
        if (user != null) {
            try { request.setAttribute("paymentOrderIds", new data.dao.CheckoutDao().paymentIds(user.getId())); }
            catch (java.sql.SQLException ex) { log("Unable to load payment links", ex); }
        }
        request.setAttribute("title", "LensCam | Thuê máy ảnh & thiết bị chuyên nghiệp");
        request.getRequestDispatcher("/views/home.jsp").forward(request, response);
    }
    private static String validTime(String value, String fallback) {
        return value != null && value.matches("(0[89]|1[0-8]):00") ? value : fallback;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static String findImage(Product product, Set<String> resources) {
        if (resources == null) return null;
        String image = product.getImage() == null ? "" : product.getImage().replace('\\', '/');
        String filename = image.substring(image.lastIndexOf('/') + 1);
        String nameKey = normalize(product.getName());
        for (String resource : resources) {
            String candidate = resource.substring(resource.lastIndexOf('/') + 1);
            if (!filename.isEmpty() && candidate.equalsIgnoreCase(filename)) return candidate;
        }
        for (String resource : resources) {
            String candidate = resource.substring(resource.lastIndexOf('/') + 1);
            int dot = candidate.lastIndexOf('.');
            if (dot > 0 && normalize(candidate.substring(0, dot)).equals(nameKey)) return candidate;
        }
        return null;
    }
}
