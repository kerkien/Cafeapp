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

    public Order() {}

    public Order(String tableId, List<Map<String, Object>> items) {
        this.tableId = tableId;
        this.items = items;
        this.status = "Pending";
        this.timestamp = new Date();
    }

    @Exclude
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
     * Helper to get quantity from item map
     */
    @Exclude
    public static int getQuantityFromItem(Map<String, Object> itemMap) {
        Object quantityObj = itemMap.get("quantity");
        if (quantityObj instanceof Long) {
            return ((Long) quantityObj).intValue();
        } else if (quantityObj instanceof Integer) {
            return (Integer) quantityObj;
        } else if (quantityObj instanceof Double) {
            return ((Double) quantityObj).intValue();
        }
        return 1; // Default
    }

    /**
     * Helper to get price from item map
     */
    @Exclude
    public static double getPriceFromItem(Map<String, Object> itemMap) {
        Object priceObj = itemMap.get("price");
        if (priceObj instanceof Double) {
            return (Double) priceObj;
        } else if (priceObj instanceof Long) {
            return ((Long) priceObj).doubleValue();
        } else if (priceObj instanceof Integer) {
            return ((Integer) priceObj).doubleValue();
        }
        return 0.0;
    }
}