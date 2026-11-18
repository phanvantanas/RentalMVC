package service;
import jakarta.servlet.http.*;import java.math.*;import java.util.*;import java.security.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import model.*;import data.dao.Database;
public class CartSupport {
 public static RentalCart cart(HttpSession session){synchronized(session){Object value=session.getAttribute("rentalCart");if(!(value instanceof RentalCart)){value=new RentalCart();session.setAttribute("rentalCart",value);}return (RentalCart)value;}}
 public static String token(HttpSession session){synchronized(session){String token=(String)session.getAttribute("shopCsrf");if(token==null){token=newToken();session.setAttribute("shopCsrf",token);}return token;}}
 public static String newToken(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
 public static boolean validToken(HttpServletRequest request){Object token=request.getSession().getAttribute("shopCsrf");String supplied=request.getParameter("csrf");return token instanceof String&&supplied!=null&&MessageDigest.isEqual(((String)token).getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));}
 public static void prepare(HttpServletRequest request){
  RentalCart cart=cart(request.getSession());token(request.getSession());List<CartLine> lines=new ArrayList<>();Map<Integer,Product> products=new HashMap<>();Database.getProductDao().findAll().forEach(p->products.put(p.getId(),p));
  Map<Integer,Integer> stock=cart.getStart()==null?Map.of():Database.getDeviceDao().countAvailable(cart.getStart(),cart.getEnd());BigDecimal subtotal=BigDecimal.ZERO;boolean available=cart.getCount()>0;
  for(var entry:cart.getQuantities().entrySet()){
   Product product=products.get(entry.getKey());String image=null;
   if(product!=null&&product.getImage()!=null){String raw=product.getImage().replace('\\','/');String file=raw.substring(raw.lastIndexOf('/')+1);try{if(request.getServletContext().getResource("/assets/images/"+file)!=null)image=request.getContextPath()+"/assets/images/"+URLEncoder.encode(file,StandardCharsets.UTF_8).replace("+","%20");}catch(Exception ignored){}}
   CartLine line=new CartLine(entry.getKey(),entry.getValue(),stock.getOrDefault(entry.getKey(),0),product==null?"Thiết bị không còn trong danh mục":product.getName(),image,product==null?BigDecimal.ZERO:BigDecimal.valueOf(product.getPrice()),cart.getDays(),product!=null&&"active".equals(product.getStatus()));lines.add(line);subtotal=subtotal.add(line.getTotal());available&=line.isAvailable();
  }
  try{RentalCart.validateDates(cart.getStart(),cart.getEnd());}catch(IllegalArgumentException ex){available=false;}
  BigDecimal saving=subtotal.multiply(BigDecimal.valueOf(cart.getDiscount())).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);
  request.setAttribute("cart",cart);request.setAttribute("cartLines",lines);request.setAttribute("cartSubtotal",subtotal);request.setAttribute("cartSaving",saving);request.setAttribute("cartTotal",subtotal.subtract(saving));request.setAttribute("canCheckout",available);
 }
}
