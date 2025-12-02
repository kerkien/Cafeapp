// E:/CafeApp/CafeApp/app/src/main/java/com/example/cafeapp/Order.java
package com.example.cafeapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Order {

    private String id;
    private String tableId;
    private String status;
    @ServerTimestamp
    private Date timestamp;
    private List<Map<String, Object>> items;

    // A no-argument constructor is required for Firestore deserialization
    public Order() {}

    // Constructor used when creating a new order
    public Order(String tableId, List<Map<String, Object>> items) {
        this.tableId = tableId;
        this.items = items;
        this.status = "Pending"; // Default status
        this.timestamp = new Date(); // Set current time
    }

    // --- Getters and Setters ---

    @Exclude // Exclude from Firestore mapping, as we manage it manually
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    /**
     * Helper method to convert the raw list of map objects from Firestore
     * into a list of MenuItem objects.
     * The @Exclude annotation is important to tell Firestore to ignore this getter.
     */
    @Exclude
    public List<MenuItem> getItemsAsMenuItems() {
        if (items == null) {
            return new ArrayList<>(); // Return an empty list if there are no items
        }

        List<MenuItem> menuItemList = new ArrayList<>();
        // 'items' here is your List<Map<String, Object>>
        for (Map<String, Object> itemMap : items) {
            MenuItem menuItem = new MenuItem();
            menuItem.setName((String) itemMap.get("name"));

            // Firestore might return numbers as Long or Double, so we handle it safely.
            Object priceObj = itemMap.get("price");
            if (priceObj instanceof Number) {
                menuItem.setPrice(((Number) priceObj).doubleValue());
            } else {
                menuItem.setPrice(0.0); // Default value
            }

            // You can also extract other properties here if they exist in the map
            // For example: menuItem.setId((String) itemMap.get("id"));

            menuItemList.add(menuItem);
        }
        return menuItemList;
    }
}
