package service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Offline EMVCo/NAPAS payload; adapted from vietqr 1.2.0 (Casso, MIT), see licenses/vietqr.txt. */
public final class VietQr {

    private VietQr() {}

    public static String bankBin(String bank) {
        if ("BIDV".equalsIgnoreCase(bank)) {
            return "970418";
        }
        if (bank != null && bank.matches("[0-9]{6}")) {
            return bank;
        }
        throw new IllegalArgumentException("Mã ngân hàng VietQR chưa được cấu hình.");
    }

    private static String field(String id, String value) {
        if (value == null
                || value.length() > 99
                || !StandardCharsets.US_ASCII.newEncoder().canEncode(value)) {
            throw new IllegalArgumentException("Dữ liệu VietQR không hợp lệ.");
        }
        return id + String.format(Locale.ROOT, "%02d", value.length()) + value;
    }

    public static String payload(String bank, String account, BigDecimal amount, String reference) {
        if (account == null
                || !account.matches("[0-9]{4,30}")
                || amount == null
                || amount.signum() <= 0
                || amount.stripTrailingZeros().scale() > 0
                || !reference.matches("[A-Za-z0-9 ]{1,25}")) {
            throw new IllegalArgumentException("Thông tin chuyển khoản không hợp lệ.");
        }
        String value =
                field("00", "01")
                        + field("01", "12")
                        + field(
                                "38",
                                field("00", "A000000727")
                                        + field(
                                                "01",
                                                field("00", bankBin(bank)) + field("01", account))
                                        + field("02", "QRIBFTTA"))
                        + field("53", "704")
                        + field("54", amount.setScale(0).toPlainString())
                        + field("58", "VN")
                        + field("62", field("08", reference))
                        + "6304";
        int crc = 0xffff;
        for (byte b : value.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xff) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff;
            }
        }
        return value + String.format(Locale.ROOT, "%04X", crc);
    }
}
