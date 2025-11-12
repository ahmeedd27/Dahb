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

    // Getters
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

    public String getImagePath() {
        return imagePath;
    }

    // Setters مع تحقق بسيط
    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) {
            this.unitPrice = 0.0;
        } else {
            this.unitPrice = unitPrice;
        }
    }

    public void setQuantity(int quantity) {
        // نمنع كمية أقل من 1. لو تفضل أن 0 يعني حذف العنصر، استبدل السطر التالي بالسلوكية المطلوبة.
        if (quantity < 1) {
            this.quantity = 1;
        } else {
            this.quantity = quantity;
        }
    }

    // عمليات مساعدة
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
