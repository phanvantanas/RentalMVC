package filter;

import data.dao.AdminUserDao;
import data.utils.API;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import model.User;

@WebFilter(filterName="AccountAccessFilter",urlPatterns={"/*"})
public class AccountAccessFilter implements Filter {
    protected AdminUserDao dao(){return new AdminUserDao();}
    public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain)throws IOException,ServletException {
        HttpServletRequest q=(HttpServletRequest)req;HttpServletResponse s=(HttpServletResponse)res;String path=q.getServletPath();
        if(path!=null&&(path.startsWith("/assets/")||"/logout".equals(path))){chain.doFilter(req,res);return;}
        HttpSession session=q.getSession(false);
        if(session!=null&&session.getAttribute("user") instanceof User previous) {
            try {
                User current=dao().activePrincipal(previous.getId());
                if(current==null){session.invalidate();if("GET".equals(q.getMethod()))s.sendRedirect(q.getContextPath()+"/login?account=unavailable");else s.sendError(401,"Tài khoản đã bị khóa hoặc không còn tồn tại.");return;}
                session.setAttribute("user",current);session.setAttribute("username",API.getName(current.getName()));
                if(path!=null&&(path.equals("/admin")||path.startsWith("/admin/")||path.startsWith("/inc/admin/"))&&!"admin".equals(current.getRole())){s.sendError(403);return;}
            }catch(SQLException ex){s.sendError(503,"Không thể kiểm tra tài khoản. Vui lòng thử lại.");return;}
        }
        chain.doFilter(req,res);
    }
}
