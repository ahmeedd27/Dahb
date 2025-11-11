package org.example.store.model;

public class Product {
    private int id;
    private String name;
    private double price;
    private String imagePath;
    private boolean active;

    // Constructor كامل
    public Product(int id, String name, double price, String imagePath, boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
        this.active = active;
    }

    // Constructor بسيط
    public Product(int id, String name, double price) {
        this(id, name, price, null, true);
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name;
    }
}