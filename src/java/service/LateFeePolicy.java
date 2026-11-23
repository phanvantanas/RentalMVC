package service;

import java.math.*;
import java.time.*;

public final class LateFeePolicy {

    public static final String VERSION = "grace1h-10-20-40-70-v1";
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public record Quote(long seconds, int percent, BigDecimal fee) {}

    private LateFeePolicy() {}

    public static Quote calculate(LocalDateTime due, LocalDateTime actual, BigDecimal dailyRate) {
        if (due == null
                || actual == null
                || dailyRate == null
                || dailyRate.signum() < 0
                || dailyRate.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Lịch trả hoặc giá thuê không hợp lệ.");
        }
        long seconds = Math.max(0, Duration.between(due, actual).getSeconds());
        if (seconds > 3650L * 86400) {
            throw new IllegalArgumentException("Thời gian trả trễ không hợp lệ.");
        }
        long days = seconds / 86400, remainder = seconds % 86400;
        int extra =
                remainder <= 3600
                        ? 0
                        : remainder <= 10800
                                ? 10
                                : remainder <= 21600 ? 20 : remainder <= 43200 ? 40 : 70;
        int percent = Math.toIntExact(days * 70 + extra);
        BigDecimal fee =
                dailyRate
                        .multiply(BigDecimal.valueOf(percent))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        if (fee.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "Phí trả trễ vượt giới hạn lưu trữ. Vui lòng kiểm tra lại lịch trả.");
        }
        return new Quote(seconds, percent, fee);
    }
}
