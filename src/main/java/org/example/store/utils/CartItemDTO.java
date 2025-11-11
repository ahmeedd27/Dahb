package org.example.store.utils;

public class CartItemDTO {
    private int productId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private String imagePath;

    public CartItemDTO(int productId, String productName, double unitPrice, String imagePath) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = 1;
        this.imagePath = imagePath;
    }

    // Getters & Setters
    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }
}