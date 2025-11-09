package com.example.cafeapp;

public class MenuItem {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String imageBase64;

    public MenuItem() { }

    public MenuItem(String id, String name, String description, double price, String category, String imageBase64) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageBase64 = imageBase64;
    }

    public String getId() { return id; }
    public String getName() { return name != null ? name : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public double getPrice() { return price; }
    public String getCategory() { return category != null ? category : ""; }
    public String getImageBase64() { return imageBase64 != null ? imageBase64 : ""; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
