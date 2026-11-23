package model;

import java.math.BigDecimal;

public class CartLine {
    private final int id, quantity, stock;
    private final String name, image;
    private final BigDecimal price, total;
    private final boolean active;

    public CartLine(
            int id,
            int quantity,
            int stock,
            String name,
            String image,
            BigDecimal price,
            long days,
            boolean active) {
        this.id = id;
        this.quantity = quantity;
        this.stock = stock;
        this.name = name;
        this.image = image;
        this.price = price;
        this.total =
                price.multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(quantity));
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getStock() {
        return stock;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public boolean isAvailable() {
        return active && stock >= quantity;
    }
}
