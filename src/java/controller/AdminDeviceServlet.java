package controller;

import data.dao.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.SQLException;
import java.util.*;
import model.*;

@WebServlet(name="AdminDeviceServlet",urlPatterns={"/admin/devices"})
public class AdminDeviceServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        HttpSession session=request.getSession();
        synchronized(session){if(session.getAttribute("adminCsrf")==null){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);session.setAttribute("adminCsrf",HexFormat.of().formatHex(bytes));}}
        try{
            List<AdminDevice> devices=new AdminDeviceDao().findAll();Map<String,Integer> counts=new HashMap<>();Map<Integer,String> images=new HashMap<>();
            Set<String> files=getServletContext().getResourcePaths("/assets/images/");
            List<String> productImageFiles=new ArrayList<>();
            if(files!=null)for(String file:files)if(file.matches("(?i).+\\.(png|jpe?g|webp|gif)$"))productImageFiles.add(file.substring(file.lastIndexOf('/')+1));
            productImageFiles.sort(String.CASE_INSENSITIVE_ORDER);request.setAttribute("productImageFiles",productImageFiles);
            for(AdminDevice d:devices){
                counts.merge(d.getEffectiveStatus(),1,Integer::sum);
                String raw=d.getImage()==null?"":d.getImage().replace('\\','/');String filename=raw.substring(raw.lastIndexOf('/')+1);
                if(files!=null)for(String file:files){String actual=file.substring(file.lastIndexOf('/')+1);if(!filename.isEmpty()&&actual.equalsIgnoreCase(filename)){images.put(d.getId(),request.getContextPath()+"/assets/images/"+URLEncoder.encode(actual,StandardCharsets.UTF_8).replace("+","%20"));break;}}
            }
            List<Product> products=Database.getProductDao().findAll();products.removeIf(p->!"active".equals(p.getStatus()));
            request.setAttribute("devices",devices);request.setAttribute("deviceCounts",counts);request.setAttribute("deviceImages",images);request.setAttribute("products",products);request.setAttribute("categories",Database.getCategoryDao().findAll());
        }catch(SQLException ex){log("Unable to load devices",ex);response.setStatus(503);request.setAttribute("loadError","Không thể tải kho thiết bị. Vui lòng thử lại.");}
        request.setAttribute("deviceStatusLabels",Map.of("available","Sẵn sàng","renting","Đang cho thuê","maintenance","Bảo trì","reserved","Đã giữ lịch","inactive","Ngừng cho thuê"));
        request.setAttribute("adminSection","devices");request.setAttribute("flash",session.getAttribute("deviceFlash"));request.setAttribute("flashError",session.getAttribute("deviceFlashError"));session.removeAttribute("deviceFlash");session.removeAttribute("deviceFlashError");
        request.getRequestDispatcher("/WEB-INF/views/admin/devices.jsp").forward(request,response);
    }
    protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        request.setCharacterEncoding("UTF-8");String token=request.getParameter("csrf");Object expected=request.getSession().getAttribute("adminCsrf");
        if(!(expected instanceof String)||token==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),((String)expected).getBytes(StandardCharsets.UTF_8))){response.sendError(403);return;}
        try{
            String action=request.getParameter("action");AdminDeviceDao dao=new AdminDeviceDao();
            if("create_product".equals(action)) {
                String image=Objects.toString(request.getParameter("image"),"").trim();
                Set<String> files=getServletContext().getResourcePaths("/assets/images/");
                if(!image.isEmpty()&&(files==null||!files.contains("/assets/images/"+image)||!image.matches("(?i).+\\.(png|jpe?g|webp|gif)$")))throw new IllegalArgumentException("Vui lòng chọn ảnh có sẵn trong thư mục assets/images.");
                new AdminProductDao().create(Integer.parseInt(request.getParameter("id_category")),Objects.toString(request.getParameter("name"),"").trim(),image,new BigDecimal(Objects.toString(request.getParameter("price"),"")),Objects.toString(request.getParameter("description"),"").trim(),request.getParameter("product_status"),Objects.toString(request.getParameter("serial"),"").trim(),request.getParameter("status"));
            }else if("create".equals(action)||"edit".equals(action)){
                Integer id="edit".equals(action)?Integer.valueOf(request.getParameter("id")):null;
                dao.save(id,Integer.parseInt(request.getParameter("product_id")),Objects.toString(request.getParameter("serial"),"").trim(),request.getParameter("status"));
            }else{
                if(action==null)throw new IllegalArgumentException("Thiếu thao tác thiết bị.");
                String[] selected=request.getParameterValues("ids");if(selected==null||selected.length==0||selected.length>100)throw new IllegalArgumentException("Chọn từ 1 đến 100 thiết bị.");
                List<Integer> ids=Arrays.stream(selected).map(Integer::valueOf).distinct().sorted().toList();
                BigDecimal price="price".equals(action)?new BigDecimal(Objects.toString(request.getParameter("price"),"")):null;
                dao.bulk(ids,action,request.getParameter("status"),price);
            }
            request.getSession().setAttribute("deviceFlash","Đã cập nhật kho thiết bị thành công.");
        }catch(IllegalArgumentException ex){request.getSession().setAttribute("deviceFlashError",ex instanceof NumberFormatException?"Dữ liệu số không hợp lệ.":ex.getMessage());
        }catch(SQLException ex){log("Unable to update devices",ex);request.getSession().setAttribute("deviceFlashError","Không thể lưu thay đổi. Vui lòng kiểm tra serial trùng hoặc thử lại.");}
        response.sendRedirect(request.getContextPath()+"/admin/devices");
    }
}
