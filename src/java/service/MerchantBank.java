package service;

import jakarta.servlet.ServletContext;

import java.io.*;
import java.util.*;

public record MerchantBank(String code, String account, String holder) {

    public static MerchantBank load(ServletContext context) throws IOException {
        Properties p = new Properties();
        try (InputStream in = context.getResourceAsStream("/WEB-INF/config/vietqr.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return new MerchantBank(
                p.getProperty("bank.code", ""),
                p.getProperty("bank.account", ""),
                p.getProperty("bank.holder", ""));
    }

    public boolean configured() {
        return ("BIDV".equalsIgnoreCase(code) || code.matches("[0-9]{6}"))
                && account.matches("[0-9]{4,30}")
                && !holder.isBlank()
                && holder.length() <= 120;
    }
}
