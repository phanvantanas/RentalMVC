package controller;

import data.dao.MyRentalDao;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import model.User;

@WebServlet(name="MyRentalServlet",urlPatterns={"/my-rentals"})
public class MyRentalServlet extends HttpServlet {
    protected MyRentalDao dao(){return new MyRentalDao();}
    @Override protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException {
        s.setHeader("Cache-Control","no-store");
        Integer id=null;
        try {if(q.getParameter("order")!=null){id=Integer.valueOf(q.getParameter("order"));if(id<=0)throw new NumberFormatException();}}
        catch(NumberFormatException ex){s.sendError(400);return;}
        if(!(q.getSession().getAttribute("user") instanceof User user)) {
            q.getSession().setAttribute("afterLogin","/my-rentals"+(id==null?"":"?order="+id));s.sendRedirect(q.getContextPath()+"/login");return;
        }
        try {
            var orders=dao().findOwned(user.getId(),id);
            if(id!=null&&orders.isEmpty()){s.sendError(404);return;}
            q.setAttribute("myOrders",orders);q.setAttribute("detailOrder",id!=null);
        } catch(SQLException ex) {log("Cannot load customer rentals",ex);s.setStatus(503);q.setAttribute("loadError","Chưa tải được đơn thuê. Vui lòng thử lại sau.");}
        q.setAttribute("rentalLabels",Map.of("pending","Chờ duyệt","confirmed","Chờ nhận máy","renting","Đang thuê","returned","Đã trả máy","cancelled","Đã hủy"));
        q.setAttribute("paymentLabels",Map.of("pending","Chờ chuyển khoản","reported","Chờ đối soát","paid","Đã nhận tiền"));
        q.getRequestDispatcher("/WEB-INF/views/shop/my-rentals.jsp").forward(q,s);
    }
}
