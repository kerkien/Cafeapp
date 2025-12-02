// E:/CafeApp/CafeApp/app/src/main/java/com/example/cafeapp/OrderAdapter.java
package com.example.cafeapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends ArrayAdapter<Order> {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public OrderAdapter(Context context, List<Order> orders) {
        super(context, 0, orders);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.order_list_item, parent, false);
        }

        Order order = getItem(position);

        TextView txtOrderTable = convertView.findViewById(R.id.txtOrderTable);
        TextView txtOrderStatus = convertView.findViewById(R.id.txtOrderStatus);
        TextView txtOrderItems = convertView.findViewById(R.id.txtOrderItems);
        TextView txtOrderTimestamp = convertView.findViewById(R.id.txtOrderTimestamp);
        Button btnStartPreparing = convertView.findViewById(R.id.btnStartPreparing);
        Button btnFinishOrder = convertView.findViewById(R.id.btnFinishOrder);

        if (order != null) {
            txtOrderTable.setText("Table: " + order.getTableId().replace("Table_", ""));

            // Set Status and Color
            String status = order.getStatus();
            txtOrderStatus.setText(status);
            if (status != null) {
                switch (status) {
                    case "Preparing":
                        txtOrderStatus.setTextColor(Color.BLUE);
                        btnStartPreparing.setVisibility(View.GONE);
                        btnFinishOrder.setVisibility(View.VISIBLE);
                        break;
                    case "Finished":
                        txtOrderStatus.setTextColor(Color.GREEN);
                        btnStartPreparing.setVisibility(View.GONE);
                        btnFinishOrder.setVisibility(View.GONE);
                        break;
                    case "Pending":
                    default:
                        txtOrderStatus.setTextColor(Color.parseColor("#FFA500")); // Orange
                        btnStartPreparing.setVisibility(View.VISIBLE);
                        btnFinishOrder.setVisibility(View.GONE);
                        break;
                }
            }

            // Set Items List
            StringBuilder itemsStr = new StringBuilder();
            // *** THIS IS THE FIX: Use the helper method to get a correctly typed list ***
            for (MenuItem item : order.getItemsAsMenuItems()) {
                if (item.getName() != null) {
                    itemsStr.append("- ").append(item.getName()).append("\n");
                }
            }
            txtOrderItems.setText(itemsStr.toString().trim());

            // Set Timestamp
            if (order.getTimestamp() != null) {
                txtOrderTimestamp.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(order.getTimestamp()));
            } else {
                txtOrderTimestamp.setText("");
            }

            // Set Button Listeners
            btnStartPreparing.setOnClickListener(v -> {
                updateOrderStatus(order, "Preparing");
            });

            btnFinishOrder.setOnClickListener(v -> {
                updateOrderStatus(order, "Finished");
            });
        }

        return convertView;
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getId() != null) {
            db.collection("orders").document(order.getId()).update("status", newStatus);
        }
    }
}
