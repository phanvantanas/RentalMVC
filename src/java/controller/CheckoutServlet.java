package controller;
import jakarta.servlet.*;import jakarta.servlet.http.*;import jakarta.servlet.annotation.WebServlet;import java.io.IOException;import java.sql.SQLException;import model.*;import service.*;import data.dao.CheckoutDao;
@WebServlet(name="CheckoutServlet",urlPatterns={"/checkout"})
public class CheckoutServlet extends HttpServlet {
 private User user(HttpServletRequest q){Object user=q.getSession().getAttribute("user");return user instanceof User?(User)user:null;}
 protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{
  s.setHeader("Cache-Control","no-store");User user=user(q);if(user==null){String order=q.getParameter("order");q.getSession().setAttribute("afterLogin",order!=null&&order.matches("[0-9]+")?"/checkout?order="+order:"/checkout");s.sendRedirect(q.getContextPath()+"/login");return;}
  synchronized(q.getSession()){
   CartSupport.token(q.getSession());String id=q.getParameter("order");
   if(id!=null){try{var payment=new CheckoutDao().payment(Integer.parseInt(id),user.getId());if(payment==null){s.sendError(404);return;}q.setAttribute("payment",payment);}catch(NumberFormatException ex){s.sendError(400);return;}catch(SQLException ex){throw new ServletException("Unable to load payment",ex);}}
   else{if(CartSupport.cart(q.getSession()).getCount()==0){s.sendRedirect(q.getContextPath()+"/cart");return;}CartSupport.prepare(q);q.getSession().setAttribute("checkoutQuote",q.getAttribute("cartTotal"));q.setAttribute("bankReady",MerchantBank.load(getServletContext()).configured());q.getSession().setAttribute("checkoutNonce",CartSupport.newToken());}
   q.setAttribute("checkoutError",q.getSession().getAttribute("checkoutError"));q.getSession().removeAttribute("checkoutError");q.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp").forward(q,s);
  }
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{
  q.setCharacterEncoding("UTF-8");User user=user(q);if(user==null){s.sendError(401);return;}if(!CartSupport.validToken(q)){s.sendError(403);return;}
  synchronized(q.getSession()){
   try{
    if("reported".equals(q.getParameter("action"))){int id=Integer.parseInt(q.getParameter("order"));if(new CheckoutDao().payment(id,user.getId())==null){s.sendError(404);return;}new CheckoutDao().report(id,user.getId());s.sendRedirect(q.getContextPath()+"/checkout?order="+id);return;}
    String nonce=q.getParameter("nonce");if(nonce==null||!nonce.equals(q.getSession().getAttribute("checkoutNonce"))){s.sendError(409,"Đơn đang được xử lý hoặc đã được gửi. Vui lòng kiểm tra lịch thuê.");return;}
    Object quote=q.getSession().getAttribute("checkoutQuote");if(!(quote instanceof java.math.BigDecimal)){s.sendError(409);return;}
    RentalCart cart=CartSupport.cart(q.getSession());int id=new CheckoutDao().place(user.getId(),cart,MerchantBank.load(getServletContext()),(java.math.BigDecimal)quote);cart.clear();q.getSession().removeAttribute("checkoutNonce");q.getSession().removeAttribute("checkoutQuote");s.sendRedirect(q.getContextPath()+"/checkout?order="+id);
   }catch(IllegalArgumentException ex){q.getSession().setAttribute("checkoutError",ex instanceof NumberFormatException?"Thông tin đơn thuê không hợp lệ.":ex.getMessage());s.sendRedirect(q.getContextPath()+"/checkout");}
   catch(SQLException ex){log("Checkout failed",ex);q.getSession().setAttribute("checkoutError","Không thể tạo đơn thuê. Vui lòng kiểm tra lại và thử lần nữa.");s.sendRedirect(q.getContextPath()+"/checkout");}
  }
 }
}
