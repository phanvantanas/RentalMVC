package model;

import java.sql.Date;
import java.sql.Timestamp;

public class Rental {

    private int id;
    private int userId;
    private Date startDate;
    private Date endDate;
    private String status; // pending, confirmed, renting, returned, cancelled
    private double total;
    private double deposit;
    private double lateFee;
    private Timestamp createdAt;

    public Rental() {}

    public Rental(
            int id,
            int userId,
            Date startDate,
            Date endDate,
            String status,
            double total,
            double deposit,
            double lateFee) {
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.total = total;
        this.deposit = deposit;
        this.lateFee = lateFee;
    }

    public Rental(
            int id,
            int userId,
            Date startDate,
            Date endDate,
            String status,
            double total,
            double deposit,
            double lateFee,
            Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.total = total;
        this.deposit = deposit;
        this.lateFee = lateFee;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }

    public double getLateFee() {
        return lateFee;
    }

    public void setLateFee(double lateFee) {
        this.lateFee = lateFee;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
