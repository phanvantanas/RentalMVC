package model;

public class Product {
    private int id;
    private int id_category;
    private String name;
    private String image;
    private double price;       // Giá thuê / ngày
    private double deposit;     // Tiền đặt cọc
    private boolean depositConfigured;
    private String description;
    private String status;      // active / inactive

    public Product() {
    }

    public Product(int id, int id_category, String name, String image, double price, double deposit, String description, String status) {
        this.id = id;
        this.id_category = id_category;
        this.name = name;
        this.image = image;
        this.price = price;
        this.deposit = deposit;
        this.depositConfigured = true;
        this.description = description;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_category() {
        return id_category;
    }

    public void setId_category(int id_category) {
        this.id_category = id_category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
        this.depositConfigured = true;
    }

    public boolean isDepositConfigured() {
        return depositConfigured;
    }

    public void setDepositConfigured(boolean depositConfigured) {
        this.depositConfigured = depositConfigured;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
