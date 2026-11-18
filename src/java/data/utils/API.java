package data.utils;

public class API {
    public static String getName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] a = fullName.trim().split("\\s+");
        return a[a.length - 1];
    }
}
