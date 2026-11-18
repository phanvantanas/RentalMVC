package controller;

import data.dao.Database;
import data.dao.DataAccessException;
import data.impl.UserImpl;
import data.utils.API;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.User;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("title", "Đăng nhập");
        request.setAttribute("loginIdentifier", request.getSession().getAttribute("loginIdentifier"));
        request.getSession().removeAttribute("loginIdentifier");
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
        request.getSession().removeAttribute("success_register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String emailphone = UserImpl.normalizeIdentifier(request.getParameter("emailphone"));
        String password = request.getParameter("password");

        request.getSession().setAttribute("loginIdentifier", emailphone);
        User user;
        try {
            user = authenticate(emailphone, password);
        } catch (DataAccessException ex) {
            log("Authentication database unavailable", ex);
            request.getSession().setAttribute("error_login", "Không thể kết nối hệ thống đăng nhập. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (user == null) {
            request.getSession().setAttribute("error_login", "Thông tin đăng nhập không chính xác!");
            response.sendRedirect("login");
        } else {
            request.getSession().removeAttribute("error_login");
            request.getSession().removeAttribute("loginIdentifier");
            request.changeSessionId();
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("username", API.getName(user.getName()));

            Object afterLogin = request.getSession().getAttribute("afterLogin");
            if (afterLogin instanceof String && ((String) afterLogin).matches("/(?:checkout|my-rentals)(?:\\?order=[0-9]+)?")) {
                request.getSession().removeAttribute("afterLogin");
                response.sendRedirect(request.getContextPath() + afterLogin);
                return;
            }

            if ("admin".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect("admin");
            } else {
                response.sendRedirect("home");
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "LoginServlet - Handles customer/admin login";
    }

    protected User authenticate(String identifier, String password) {
        return Database.getUserDao().findUser(identifier, password);
    }
}
