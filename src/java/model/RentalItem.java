package model;

public class RentalItem {
    private int id;
    private int rentalId;
    private int deviceId;
    private double pricePerDay;

    public RentalItem() {
    }

    public RentalItem(int id, int rentalId, int deviceId, double pricePerDay) {
        this.id = id;
        this.rentalId = rentalId;
        this.deviceId = deviceId;
        this.pricePerDay = pricePerDay;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRentalId() {
        return rentalId;
    }

    public void setRentalId(int rentalId) {
        this.rentalId = rentalId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }
}
