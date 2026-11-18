package model;

import java.math.BigDecimal;

public class AdminDevice {
    private final int id, productId, categoryId;
    private final String serial, status, productName, image, productStatus, categoryName;
    private final BigDecimal price;
    private final int rentalId, reservations;
    public AdminDevice(int id, int productId, int categoryId, String serial, String status, String productName,
            String image, String productStatus, String categoryName, BigDecimal price, int rentalId, int reservations) {
        this.id=id; this.productId=productId; this.categoryId=categoryId; this.serial=serial; this.status=status;
        this.productName=productName; this.image=image; this.productStatus=productStatus; this.categoryName=categoryName;
        this.price=price; this.rentalId=rentalId; this.reservations=reservations;
    }
    public int getId(){return id;} public int getProductId(){return productId;} public int getCategoryId(){return categoryId;}
    public String getSerial(){return serial;} public String getStatus(){return status;} public String getProductName(){return productName;}
    public String getImage(){return image;} public String getProductStatus(){return productStatus;} public String getCategoryName(){return categoryName;}
    public BigDecimal getPrice(){return price;} public int getRentalId(){return rentalId;} public int getReservations(){return reservations;}
    public String getEffectiveStatus(){
        if(rentalId>0)return "renting";
        if("maintenance".equals(status))return "maintenance";
        if(!"active".equals(productStatus))return "inactive";
        if(reservations>0)return "reserved";
        return "available";
    }
}
