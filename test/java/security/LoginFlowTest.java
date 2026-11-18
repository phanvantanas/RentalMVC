package security;
import controller.LoginServlet;import data.dao.DataAccessException;import data.impl.UserImpl;import jakarta.servlet.http.*;import java.lang.reflect.Proxy;import java.util.*;import model.User;
public class LoginFlowTest {
 static int checks;
 static class Endpoint extends LoginServlet {
  User result;boolean unavailable;String received;String password;
  protected User authenticate(String id,String pass){received=id;password=pass;if(unavailable)throw new DataAccessException("Test database unavailable");return result;}
  public void log(String message,Throwable cause){}
  void post(HttpServletRequest q,HttpServletResponse s)throws Exception{super.doPost(q,s);}
 }
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
 public static void main(String[]a)throws Exception{
  check("test@example.com".equals(UserImpl.normalizeIdentifier("  TEST@EXAMPLE.COM  ")),"Normalize email");
  check("0912345678".equals(UserImpl.normalizeIdentifier(" 0912 345 678 ")),"Normalize phone");
  for(int mode=0;mode<4;mode++){
   Endpoint e=new Endpoint();if(mode>=2)e.result=new User(1,"Preview User","","","",mode==2?"admin":"user");e.unavailable=mode==1;
   var session=AdminSecurityTest.session(null,null);var base=AdminSecurityTest.request(session,Map.of("emailphone","  TEST@EXAMPLE.COM ","password"," Exact Password "));
   List<String> encoding=new ArrayList<>();int[] rotations={0};
   HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(LoginFlowTest.class.getClassLoader(),new Class<?>[]{HttpServletRequest.class},(p,m,args)->{
    if(m.getName().equals("setCharacterEncoding")){encoding.add((String)args[0]);return null;}
    if(m.getName().equals("changeSessionId")){rotations[0]++;return "rotated";}
    return m.invoke(base,args);
   });
   var state=new AdminSecurityTest.ResponseState();e.post(request,AdminSecurityTest.response(state));
   check(encoding.equals(List.of("UTF-8")),"UTF-8 before reading fields");check("test@example.com".equals(e.received),"Normalized identifier reaches DAO");check(" Exact Password ".equals(e.password),"Password preserved exactly");
   if(mode==0)check(String.valueOf(session.getAttribute("error_login")).contains("không chính xác"),"Credential error");
   if(mode==1)check(String.valueOf(session.getAttribute("error_login")).contains("Không thể kết nối"),"Database error distinguished");
   if(mode>=2){check(rotations[0]==1,"Session rotated");check(e.result==session.getAttribute("user"),"Principal saved");check((mode==2?"admin":"home").equals(state.location),"Role redirect");}
  }
  for(String destination:List.of("/my-rentals","/my-rentals?order=5","/checkout?order=5","https://example.com","//example.com","/my-rentals?order=5&user_id=2")){
   Endpoint e=new Endpoint();e.result=new User(1,"Preview User","","","","user");var session=AdminSecurityTest.session(null,null);session.setAttribute("afterLogin",destination);var state=new AdminSecurityTest.ResponseState();e.post(AdminSecurityTest.request(session,Map.of("emailphone","test@example.com","password","fixture")),AdminSecurityTest.response(state));boolean safe=destination.equals("/my-rentals")||destination.equals("/my-rentals?order=5")||destination.equals("/checkout?order=5");check((safe?"/RentalCamera"+destination:"home").equals(state.location),"Tracking redirect allowlist: "+destination);
  }
  System.out.println("PASS: "+checks+" login regression checks; no database writes.");
 }
}
