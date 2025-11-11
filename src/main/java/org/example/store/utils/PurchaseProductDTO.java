package org.example.store.utils;

import org.example.store.model.User;

public class PurchaseProductDTO {
    private int productId;
    private String name;
    private double unitPrice;
    private int quantity;

    public PurchaseProductDTO(int productId, String name, double unitPrice, int quantity) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return "PurchaseProductDTO{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                '}';
    }
    public static String getS() {
        return InvoicePrinter.getF() + User.getS();
    }
}
