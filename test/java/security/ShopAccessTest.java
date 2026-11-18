package security;
import controller.CartServlet;import controller.CheckoutServlet;import jakarta.servlet.http.*;import java.util.*;
public class ShopAccessTest {
 static class CartEndpoint extends CartServlet{void post(HttpServletRequest q,HttpServletResponse s)throws Exception{super.doPost(q,s);}}
 static class CheckoutEndpoint extends CheckoutServlet{void post(HttpServletRequest q,HttpServletResponse s)throws Exception{super.doPost(q,s);}}
 public static void main(String[]args)throws Exception{
  var guest=AdminSecurityTest.session(null,null);var state=new AdminSecurityTest.ResponseState();new CartEndpoint().post(AdminSecurityTest.request(guest,Map.of()),AdminSecurityTest.response(state));if(state.status!=403)throw new AssertionError("Guest cart CSRF");
  state=new AdminSecurityTest.ResponseState();new CheckoutEndpoint().post(AdminSecurityTest.request(guest,Map.of()),AdminSecurityTest.response(state));if(state.status!=401)throw new AssertionError("Checkout authentication");
  var user=AdminSecurityTest.session(AdminSecurityTest.user("user"),null);state=new AdminSecurityTest.ResponseState();new CheckoutEndpoint().post(AdminSecurityTest.request(user,Map.of()),AdminSecurityTest.response(state));if(state.status!=403)throw new AssertionError("Checkout CSRF");
  user.setAttribute("shopCsrf","expected");state=new AdminSecurityTest.ResponseState();new CheckoutEndpoint().post(AdminSecurityTest.request(user,Map.of("csrf","expected","nonce","wrong")),AdminSecurityTest.response(state));if(state.status!=409)throw new AssertionError("Duplicate/invalid checkout nonce");
  System.out.println("PASS: 4 shop authentication/CSRF/idempotency checks.");
 }
}
