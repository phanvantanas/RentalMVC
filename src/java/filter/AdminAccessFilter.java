package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

@WebFilter(filterName = "AdminAccessFilter", urlPatterns = {"/admin", "/admin/*", "/inc/admin/*"})
public class AdminAccessFilter implements Filter {
    @Override
    public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object principal = session == null ? null : session.getAttribute("user");
        if (!(principal instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"admin".equalsIgnoreCase(((User) principal).getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ quản trị viên được truy cập khu vực này.");
            return;
        }
        chain.doFilter(req, res);
    }
}
