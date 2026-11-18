package controller;

import data.dao.AdminUserDao;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import model.User;

@WebServlet(name="AdminUserServlet",urlPatterns={"/admin/users"})
public class AdminUserServlet extends HttpServlet {
    protected AdminUserDao dao(){return new AdminUserDao();}
    private User admin(HttpServletRequest q,HttpServletResponse s)throws IOException {
        if(!(q.getSession().getAttribute("user") instanceof User user)){s.sendError(401);return null;}
        if(!"admin".equals(user.getRole())){s.sendError(403);return null;}return user;
    }
    @Override protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException {
        if(admin(q,s)==null)return;HttpSession session=q.getSession();
        synchronized(session){if(session.getAttribute("adminCsrf")==null)session.setAttribute("adminCsrf",service.CartSupport.newToken());}
        try {
            var users=dao().findAll();Map<String,Integer> stats=new HashMap<>();stats.put("total",users.size());
            for(var user:users){stats.merge(user.get("role").toString(),1,Integer::sum);if("locked".equals(user.get("status")))stats.merge("locked",1,Integer::sum);}
            q.setAttribute("users",users);q.setAttribute("userStats",stats);
        }catch(SQLException ex){log("Cannot load admin users",ex);s.setStatus(503);q.setAttribute("loadError","Không thể tải danh sách người dùng. Vui lòng thử lại.");}
        q.setAttribute("adminSection","users");q.setAttribute("userFlash",session.getAttribute("userFlash"));q.setAttribute("userError",session.getAttribute("userError"));q.setAttribute("userDraft",session.getAttribute("userDraft"));session.removeAttribute("userFlash");session.removeAttribute("userError");session.removeAttribute("userDraft");
        q.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(q,s);
    }
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException {
        q.setCharacterEncoding("UTF-8");User actor=admin(q,s);if(actor==null)return;
        String supplied=q.getParameter("csrf");Object expected=q.getSession().getAttribute("adminCsrf");
        if(!(expected instanceof String)||supplied==null||!MessageDigest.isEqual(((String)expected).getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))){s.sendError(403);return;}
        String action=q.getParameter("action");
        try {
            if("create".equals(action)||"edit".equals(action)) {
                Integer id="edit".equals(action)?Integer.valueOf(q.getParameter("id")):null;
                if(id==null&&!Objects.equals(q.getParameter("password"),q.getParameter("confirm")))throw new IllegalArgumentException("Nhập lại mật khẩu chưa khớp.");
                dao().save(actor.getId(),id,trim(q,"name"),trim(q,"email").toLowerCase(Locale.ROOT),trim(q,"phone"),q.getParameter("role"),q.getParameter("status"),q.getParameter("password"));
            } else {
                String[] raw=q.getParameterValues("ids");if(raw==null||raw.length==0||raw.length>100)throw new IllegalArgumentException("Chọn từ 1 đến 100 tài khoản.");
                dao().bulk(actor.getId(),Arrays.stream(raw).map(Integer::valueOf).distinct().sorted().toList(),action);
            }
            q.getSession().setAttribute("userFlash","Đã cập nhật người dùng thành công.");q.getSession().removeAttribute("userDraft");
        }catch(IllegalArgumentException ex){error(q,action,ex instanceof NumberFormatException?"Mã tài khoản không hợp lệ.":ex.getMessage());}
        catch(SQLIntegrityConstraintViolationException ex){error(q,action,"Email đã tồn tại hoặc tài khoản còn dữ liệu liên quan. Vui lòng kiểm tra lại.");}
        catch(SQLException ex){log("Cannot update users",ex);error(q,action,"Không thể lưu tài khoản. Vui lòng thử lại.");}
        s.sendRedirect(q.getContextPath()+"/admin/users");
    }
    private static String trim(HttpServletRequest q,String name){return Objects.toString(q.getParameter(name),"").trim();}
    private static void error(HttpServletRequest q,String action,String message) {
        q.getSession().setAttribute("userError",message);
        if("create".equals(action)||"edit".equals(action)){Map<String,String> draft=new HashMap<>();for(String key:List.of("id","name","email","phone","role","status"))draft.put(key,trim(q,key));draft.put("action",action);q.getSession().setAttribute("userDraft",draft);}
    }
}
