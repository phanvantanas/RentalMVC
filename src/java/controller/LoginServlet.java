package controller;

import data.dao.Database;
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
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
        request.getSession().removeAttribute("success_register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String emailphone = request.getParameter("emailphone");
        String password = request.getParameter("password");

        User user = Database.getUserDao().findUser(emailphone, password);
        if (user == null) {
            request.getSession().setAttribute("error_login", "Thông tin đăng nhập không chính xác!");
            response.sendRedirect("login");
        } else {
            request.getSession().removeAttribute("error_login");
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("username", API.getName(user.getName()));

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
}
