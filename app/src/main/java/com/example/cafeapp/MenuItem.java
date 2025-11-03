package com.example.cafeapp;

public class MenuItem {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;

    public MenuItem() {
        // Empty constructor required by Firebase
    }

    public MenuItem(String id, String name, String description, double price, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
}
