package model;

public class Device {
    private int id;
    private int productId;
    private String serial;
    private String status; // available / maintenance

    public Device() {}

    public Device(int id, int productId, String serial, String status) {
        this.id = id;
        this.productId = productId;
        this.serial = serial;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
