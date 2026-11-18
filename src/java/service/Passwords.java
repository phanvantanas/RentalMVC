package service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
    private static final int ITERATIONS=600000;
    private Passwords(){}
    public static String hash(String password) {
        if(password==null||password.length()<8||password.length()>128)throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 128 ký tự.");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2-sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean verify(String supplied,String stored) {
        if(supplied==null||stored==null)return false;
        if(!stored.startsWith("pbkdf2-sha256$"))return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),stored.getBytes(StandardCharsets.UTF_8));
        try {
            String[] parts=stored.split("\\$",-1);if(parts.length!=4)return false;
            int rounds=Integer.parseInt(parts[1]);if(rounds<100000||rounds>1000000||supplied.length()>128)return false;
            byte[] salt=Base64.getDecoder().decode(parts[2]),expected=Base64.getDecoder().decode(parts[3]);if(salt.length!=16||expected.length!=32)return false;
            return MessageDigest.isEqual(expected,derive(supplied,salt,rounds));
        } catch(IllegalArgumentException ex){return false;}
    }
    private static byte[] derive(String password,byte[] salt,int rounds) {
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,rounds,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(GeneralSecurityException ex){throw new IllegalStateException("Password hashing unavailable",ex);}
        finally{spec.clearPassword();}
    }
}
