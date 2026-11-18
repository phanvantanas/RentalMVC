package controller;
import jakarta.servlet.*;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;import java.time.LocalDate;import model.*;import service.CartSupport;import data.dao.Database;
@WebServlet(name="CartServlet",urlPatterns={"/cart"})
public class CartServlet extends HttpServlet {
 protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{
  s.setHeader("Cache-Control","no-store");synchronized(q.getSession()){CartSupport.prepare(q);q.setAttribute("cartError",q.getSession().getAttribute("cartError"));q.getSession().removeAttribute("cartError");q.getRequestDispatcher("/WEB-INF/views/shop/cart.jsp").forward(q,s);}
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{
  q.setCharacterEncoding("UTF-8");if(!CartSupport.validToken(q)){s.sendError(403);return;}String action=q.getParameter("action");String destination="/cart";
  synchronized(q.getSession()){
   RentalCart cart=CartSupport.cart(q.getSession());
   try{
    if("clear".equals(action))cart.clear();
    else if("remove".equals(action))cart.remove(Integer.parseInt(q.getParameter("product")));
    else if("update".equals(action))cart.update(Integer.parseInt(q.getParameter("product")),Integer.parseInt(q.getParameter("quantity")));
    else if("add".equals(action)||"buy".equals(action)){
     int id=Integer.parseInt(q.getParameter("product"));LocalDate start=LocalDate.parse(q.getParameter("start")),end=LocalDate.parse(q.getParameter("end"));RentalCart.validateDates(start,end);
     Product product=Database.getProductDao().find(id);if(product==null||!"active".equals(product.getStatus()))throw new IllegalArgumentException("Thiết bị không còn cho thuê.");
     int required=cart.getQuantities().getOrDefault(id,0)+1;int stock=Database.getDeviceDao().countAvailable(start,end).getOrDefault(id,0);if(stock<required)throw new IllegalArgumentException("Không đủ thiết bị trống cho lịch thuê đã chọn.");
     cart.add(id,1,start,end,q.getParameter("pickup_time"),q.getParameter("return_time"));if("buy".equals(action))destination="/checkout";
    }else throw new IllegalArgumentException("Thao tác giỏ hàng không hợp lệ.");
    q.getSession().removeAttribute("checkoutNonce");q.getSession().removeAttribute("checkoutQuote");
   }catch(RuntimeException ex){q.getSession().setAttribute("cartError",ex instanceof IllegalArgumentException&&!(ex instanceof NumberFormatException)?ex.getMessage():"Thông tin giỏ hàng không hợp lệ.");destination="/cart";}
  }
  s.sendRedirect(q.getContextPath()+destination);
 }
}
