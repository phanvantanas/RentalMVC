package controller;
import jakarta.servlet.*;import jakarta.servlet.annotation.*;import jakarta.servlet.http.*;import java.io.*;import java.sql.SQLException;import model.User;import service.*;import data.dao.ReceiptDao;
@WebServlet(name="ReceiptServlet",urlPatterns={"/payment/receipt"})
@MultipartConfig(fileSizeThreshold=1048576,maxFileSize=5242880,maxRequestSize=6291456)
public class ReceiptServlet extends HttpServlet {
 private User user(HttpServletRequest q){Object value=q.getSession().getAttribute("user");return value instanceof User?(User)value:null;}
 protected void doGet(HttpServletRequest q,HttpServletResponse s)throws IOException,ServletException{
  User user=user(q);if(user==null){s.sendError(401);return;}
  try{int id=Integer.parseInt(q.getParameter("order"));var image=new ReceiptDao().find(id,user.getId(),"admin".equalsIgnoreCase(user.getRole()));if(image==null){s.sendError(404);return;}
   if(!"image/png".equals(image.contentType())&&!"image/jpeg".equals(image.contentType())){s.sendError(415);return;}
   s.setHeader("Cache-Control","private, no-store");s.setHeader("X-Content-Type-Options","nosniff");s.setContentType(image.contentType());s.setHeader("Content-Disposition","inline; filename=receipt-"+id+("image/png".equals(image.contentType())?".png":".jpg"));s.setContentLength(image.bytes().length);s.getOutputStream().write(image.bytes());
  }catch(NumberFormatException ex){s.sendError(400);}catch(SQLException ex){log("Unable to load receipt",ex);s.sendError(503);}
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse s)throws IOException,ServletException{
  q.setCharacterEncoding("UTF-8");User user=user(q);if(user==null){s.sendError(401);return;}int id=0;String query=q.getQueryString();if(query!=null&&query.matches("order=[0-9]{1,9}")){id=Integer.parseInt(query.substring(6));}
  try{
   if(!CartSupport.validToken(q)){s.sendError(403);return;}id=Integer.parseInt(q.getParameter("order"));if(q.getContentType()==null||!q.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data"))throw new IllegalArgumentException("Vui lòng chọn ảnh chuyển khoản để tải lên.");Part file=q.getPart("receipt");if(file==null||file.getSize()==0)throw new IllegalArgumentException("Vui lòng chọn ảnh chuyển khoản.");if(file.getSize()>ReceiptImage.MAX_BYTES)throw new IllegalArgumentException("Ảnh tối đa 5 MB.");
   try(InputStream in=file.getInputStream()){byte[] data=in.readNBytes(ReceiptImage.MAX_BYTES+1);new ReceiptDao().save(id,user.getId(),ReceiptImage.validate(data));}finally{try{file.delete();}catch(IOException cleanup){log("Unable to remove temporary receipt upload",cleanup);}}
  }catch(IllegalStateException ex){q.getSession().setAttribute("checkoutError","Ảnh tải lên quá lớn. Vui lòng chọn ảnh PNG/JPG tối đa 5 MB.");}
  catch(IllegalArgumentException ex){q.getSession().setAttribute("checkoutError",ex instanceof NumberFormatException?"Đơn thuê không hợp lệ.":ex.getMessage());}
  catch(SQLException ex){log("Unable to save receipt",ex);q.getSession().setAttribute("checkoutError","Không thể lưu ảnh chuyển khoản. Vui lòng thử lại.");}
  catch(IOException ex){log("Unable to decode receipt",ex);q.getSession().setAttribute("checkoutError","Không đọc được ảnh. Vui lòng chọn ảnh PNG/JPG hợp lệ.");}
  s.sendRedirect(q.getContextPath()+"/checkout"+(id>0?"?order="+id:""));
 }
}
