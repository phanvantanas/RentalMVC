package model;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AdminRental {

    private final int id;
    private final String customerName, phone, email, status;
    private final Date startDate, endDate;
    private final double total, lateFee;
    private final List<Line> items = new ArrayList<>();
    private String paymentStatus, transferReference;
    private boolean receiptAvailable;

    public boolean isReceiptAvailable() {
        return receiptAvailable;
    }

    public void setReceiptAvailable(boolean value) {
        receiptAvailable = value;
    }

    private String pickupTime, returnTime;

    public String getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(String value) {
        pickupTime = value == null ? null : value.substring(0, 5);
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String value) {
        returnTime = value == null ? null : value.substring(0, 5);
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String value) {
        paymentStatus = value;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setTransferReference(String value) {
        transferReference = value;
    }

    public AdminRental(
            int id,
            String customerName,
            String phone,
            String email,
            String status,
            Date startDate,
            Date endDate,
            double total,
            double lateFee) {
        this.id = id;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.total = total;
        this.lateFee = lateFee;
    }

    public int getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public double getTotal() {
        return total;
    }

    public double getLateFee() {
        return lateFee;
    }

    public List<Line> getItems() {
        return items;
    }

    public long getDays() {
        return startDate == null || endDate == null
                ? 0
                : Math.max(
                        1,
                        java.time.temporal.ChronoUnit.DAYS.between(
                                startDate.toLocalDate(), endDate.toLocalDate()));
    }

    public double getDailyRate() {
        return items.stream().mapToDouble(Line::getPrice).sum();
    }

    public boolean isOverdue() {
        return "renting".equals(status)
                && endDate != null
                && endDate.toLocalDate()
                        .atTime(
                                returnTime == null
                                        ? java.time.LocalTime.of(17, 0)
                                        : java.time.LocalTime.parse(returnTime))
                        .isBefore(java.time.LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public static class Line {

        private final String name, serial;
        private final double price;

        public Line(String name, String serial, double price) {
            this.name = name;
            this.serial = serial;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public String getSerial() {
            return serial;
        }

        public double getPrice() {
            return price;
        }
    }
}
