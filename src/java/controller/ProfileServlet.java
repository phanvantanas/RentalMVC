package controller;

import data.dao.ProfileDao;
import data.impl.UserImpl;
import data.utils.API;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.User;

import service.CartSupport;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

@WebServlet(
        name = "ProfileServlet",
        urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    protected ProfileDao dao() {
        return new ProfileDao();
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        s.setHeader("Cache-Control", "no-store");
        if (!(q.getSession().getAttribute("user") instanceof User user)) {
            q.getSession().setAttribute("afterLogin", "/profile");
            s.sendRedirect(q.getContextPath() + "/login");
            return;
        }
        CartSupport.token(q.getSession());
        if (q.getAttribute("profileForm") == null) {
            q.setAttribute(
                    "profileForm",
                    Map.of(
                            "name",
                            Objects.toString(user.getName(), ""),
                            "email",
                            Objects.toString(user.getEmail(), ""),
                            "phone",
                            Objects.toString(user.getPhone(), "")));
        }
        q.setAttribute("profileSuccess", q.getSession().getAttribute("profileSuccess"));
        q.getSession().removeAttribute("profileSuccess");
        q.setAttribute("passwordSuccess", q.getSession().getAttribute("passwordSuccess"));
        q.getSession().removeAttribute("passwordSuccess");
        q.getRequestDispatcher("/WEB-INF/views/shop/profile.jsp").forward(q, s);
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse s)
            throws ServletException, IOException {
        q.setCharacterEncoding("UTF-8");
        s.setHeader("Cache-Control", "no-store");
        if (!(q.getSession().getAttribute("user") instanceof User user)) {
            s.sendError(401);
            return;
        }
        if (!CartSupport.validToken(q)) {
            s.sendError(403);
            return;
        }
        if ("change_password".equals(q.getParameter("action"))) {
            changePassword(q, s, user);
            return;
        }
        String name = Objects.toString(q.getParameter("name"), "").strip();
        String email = UserImpl.normalizeIdentifier(q.getParameter("email"));
        String phone = UserImpl.normalizeIdentifier(q.getParameter("phone"));
        try {
            User updated = dao().update(user.getId(), name, email, phone);
            q.getSession().setAttribute("user", updated);
            q.getSession().setAttribute("username", API.getName(updated.getName()));
            q.getSession().setAttribute("profileSuccess", "Đã cập nhật thông tin cá nhân.");
            s.sendRedirect(q.getContextPath() + "/profile");
            return;
        } catch (IllegalArgumentException ex) {
            s.setStatus(400);
            q.setAttribute("profileError", ex.getMessage());
        } catch (SQLIntegrityConstraintViolationException ex) {
            s.setStatus(400);
            q.setAttribute("profileError", "Email hoặc số điện thoại đã được sử dụng.");
        } catch (SQLException ex) {
            log("Unable to update profile", ex);
            s.setStatus(503);
            q.setAttribute("profileError", "Không thể lưu thông tin. Vui lòng thử lại.");
        }
        q.setAttribute("profileForm", Map.of("name", name, "email", email, "phone", phone));
        doGet(q, s);
    }

    private void changePassword(HttpServletRequest q, HttpServletResponse s, User user)
            throws ServletException, IOException {
        String current = q.getParameter("current_password"),
                next = q.getParameter("new_password"),
                confirm = q.getParameter("confirm_password");
        try {
            ProfileDao.validatePasswordChange(current, next, confirm);
            dao().changePassword(user.getId(), current, next, confirm);
            q.changeSessionId();
            q.getSession()
                    .setAttribute(
                            "passwordSuccess",
                            "Đã đổi mật khẩu thành công. Hãy dùng mật khẩu mới cho lần đăng nhập"
                                + " tiếp theo.");
            s.sendRedirect(q.getContextPath() + "/profile");
            return;
        } catch (IllegalArgumentException ex) {
            s.setStatus(400);
            q.setAttribute("passwordError", ex.getMessage());
        } catch (SQLException ex) {
            log("Unable to change password", ex);
            s.setStatus(503);
            q.setAttribute("passwordError", "Không thể đổi mật khẩu lúc này. Vui lòng thử lại.");
        }
        doGet(q, s);
    }
}
