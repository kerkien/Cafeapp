package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderTrackingActivity extends AppCompatActivity {

    private TextView txtOrderStatusClient;
    private TextView txtTotalPrice;
    private ProgressBar progressBar;
    private Button btnDoneClient;
    private ListView listOrderItems;
    private FirebaseFirestore db;
    private String orderId;
    private OrderItemAdapter orderItemAdapter;
    private List<OrderItem> orderItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        txtOrderStatusClient = findViewById(R.id.txtOrderStatusClient);
        txtTotalPrice = findViewById(R.id.txtTotalPrice);
        progressBar = findViewById(R.id.progressBar);
        btnDoneClient = findViewById(R.id.btnDoneClient);
        listOrderItems = findViewById(R.id.listOrderItems);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        orderItems = new ArrayList<>();
        orderItemAdapter = new OrderItemAdapter(this, orderItems);
        listOrderItems.setAdapter(orderItemAdapter);

        if (orderId != null) {
            loadOrderDetails();
            listenToOrderStatusChanges();
        } else {
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show();
        }

        btnDoneClient.setOnClickListener(v -> {
            Intent intent = new Intent(OrderTrackingActivity.this, Landingactivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadOrderDetails() {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                        if (items != null) {
                            orderItems.clear();
                            double total = 0.0;

                            for (Map<String, Object> item : items) {
                                String name = (String) item.get("name");
                                Double price = item.get("price") instanceof Double
                                        ? (Double) item.get("price")
                                        : ((Long) item.get("price")).doubleValue();

                                Object quantityObj = item.get("quantity");
                                int quantity = 1;
                                if (quantityObj instanceof Long) {
                                    quantity = ((Long) quantityObj).intValue();
                                } else if (quantityObj instanceof Integer) {
                                    quantity = (Integer) quantityObj;
                                }

                                orderItems.add(new OrderItem(name, quantity, price));
                                total += price * quantity;
                            }

                            orderItemAdapter.notifyDataSetChanged();
                            txtTotalPrice.setText(String.format("$%.2f", total));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderTracking", "Failed to load order details", e);
                    Toast.makeText(this, "Failed to load order details", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenToOrderStatusChanges() {
        db.collection("orders").document(orderId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error listening to order status", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        updateOrderStatus(status);
                    }
                });
    }

    private void updateOrderStatus(String status) {
        if (status == null) {
            status = "Pending";
        }

        txtOrderStatusClient.setText(status);

        switch (status) {
            case "Pending":
                progressBar.setProgress(0);
                txtOrderStatusClient.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
            case "Preparing":
                progressBar.setProgress(1);
                txtOrderStatusClient.setTextColor(getColor(android.R.color.holo_blue_dark));
                break;
            case "Ready":
                progressBar.setProgress(2);
                txtOrderStatusClient.setTextColor(getColor(android.R.color.holo_green_dark));
                break;
            default:
                progressBar.setProgress(0);
                break;
        }
    }

    // OrderItem model class
    private static class OrderItem {
        String name;
        int quantity;
        double price;

        OrderItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }

    // Adapter for order items
    private class OrderItemAdapter extends ArrayAdapter<OrderItem> {

        OrderItemAdapter(OrderTrackingActivity context, List<OrderItem> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.order_item_row, parent, false);
            }

            OrderItem item = getItem(position);
            if (item != null) {
                TextView txtItemName = convertView.findViewById(R.id.txtItemName);
                TextView txtItemQuantity = convertView.findViewById(R.id.txtItemQuantity);
                TextView txtItemPrice = convertView.findViewById(R.id.txtItemPrice);

                txtItemName.setText(item.name);
                txtItemQuantity.setText("x" + item.quantity);
                txtItemPrice.setText(String.format("$%.2f", item.price * item.quantity));
            }

            return convertView;
        }
    }
}