package controller;

import data.dao.DataAccessException;
import data.dao.Database;
import data.dao.UserDao;
import data.utils.API;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import model.User;

import java.io.IOException;

@WebServlet(
        name = "RegisterServlet",
        urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("title", "Đăng ký tài khoản");
        request.getRequestDispatcher("/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = trim(request.getParameter("name"));
        String email = trim(request.getParameter("email")).toLowerCase(java.util.Locale.ROOT);
        String phone = trim(request.getParameter("phone"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");
        if (password == null) {
            password = "";
        }

        String error = validate(name, email, phone, password, confirm);
        User newUser = null;

        if (error == null) {
            UserDao dao = Database.getUserDao();
            if (dao.existsEmail(email)) {
                error = "Email này đã được sử dụng!";
            } else if (dao.existsPhone(phone)) {
                error = "Số điện thoại này đã được sử dụng!";
            } else if (!dao.insert(new User(0, name, email, phone, password, "user"))) {
                error = "Đăng ký thất bại, vui lòng thử lại sau!";
            } else {
                try {
                    newUser = dao.findUser(email, password);
                } catch (DataAccessException ex) {
                    log("Account created but automatic sign-in is temporarily unavailable", ex);
                }
            }
        }

        if (error != null) {
            request.setAttribute("error_register", error);
            request.setAttribute("title", "Đăng ký tài khoản");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }

        if (newUser == null) {
            request.getSession()
                    .setAttribute("success_register", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.sendRedirect("login");
            return;
        }

        // Tự động đăng nhập
        request.getSession().setAttribute("user", newUser);
        request.getSession().setAttribute("username", API.getName(newUser.getName()));
        response.sendRedirect("home");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String validate(
            String name, String email, String phone, String password, String confirm) {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            return "Vui lòng điền đầy đủ các thông tin!";
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            return "Email không hợp lệ!";
        }
        if (!phone.matches("^[0-9]{9,11}$")) {
            return "Số điện thoại phải từ 9 đến 11 chữ số!";
        }
        if (password.length() < 8 || password.length() > 128) {
            return "Mật khẩu phải từ 8 đến 128 ký tự!";
        }
        if (!password.equals(confirm)) {
            return "Xác nhận mật khẩu không trùng khớp!";
        }
        return null;
    }

    @Override
    public String getServletInfo() {
        return "RegisterServlet - Handles customer registration";
    }
}
