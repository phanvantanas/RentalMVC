package controller;

import data.dao.AdminRentalDao;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

@WebServlet(name="AdminWorkflowServlet",urlPatterns={"/admin/workflow"})
public class AdminWorkflowServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException {
        q.setAttribute("adminSection","workflow");
        Map<String,Integer> counts=new HashMap<>();
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        try {
            for(var order:new AdminRentalDao().findAll()) {
                if("pending".equals(order.getStatus()))counts.merge("pending",1,Integer::sum);
                if("reported".equals(order.getPaymentStatus())&&!"cancelled".equals(order.getStatus()))counts.merge("review",1,Integer::sum);
                if("confirmed".equals(order.getStatus())&&order.getStartDate()!=null&&order.getEndDate()!=null&&!order.getStartDate().toLocalDate().isAfter(today)&&!order.getEndDate().toLocalDate().isBefore(today)&&("paid".equals(order.getPaymentStatus())||order.getPaymentStatus()==null))counts.merge("handover",1,Integer::sum);
                if("renting".equals(order.getStatus())&&order.getEndDate()!=null&&!order.getEndDate().toLocalDate().isAfter(today))counts.merge("returns",1,Integer::sum);
            }
            q.setAttribute("operationCounts",counts);
        } catch(SQLException ex) {log("Cannot load operational queues",ex);s.setStatus(503);q.setAttribute("loadError","Chưa tải được số đơn cần xử lý. Vui lòng tải lại trang.");}
        q.getRequestDispatcher("/WEB-INF/views/admin/workflow.jsp").forward(q,s);
    }
}
