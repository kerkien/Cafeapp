package com.example.cafeapp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends ArrayAdapter<Order> {

    private static final String TAG = "OrderAdapter";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        LinearLayout layoutOrderItems = convertView.findViewById(R.id.layoutOrderItems);
        TextView txtOrderTimestamp = convertView.findViewById(R.id.txtOrderTimestamp);
        Button btnStartPreparing = convertView.findViewById(R.id.btnStartPreparing);
        Button btnFinishOrder = convertView.findViewById(R.id.btnFinishOrder);

        if (order != null) {
            txtOrderTable.setText("Table: " + order.getTableId().replace("Table_", ""));

            String status = order.getStatus();
            txtOrderStatus.setText(status);
            switch (status) {
                case "Preparing":
                    txtOrderStatus.setTextColor(Color.BLUE);
                    btnStartPreparing.setVisibility(View.GONE);
                    btnFinishOrder.setVisibility(View.VISIBLE);
                    break;
                case "Ready":
                case "Finished":
                    txtOrderStatus.setTextColor(Color.GREEN);
                    btnStartPreparing.setVisibility(View.GONE);
                    btnFinishOrder.setVisibility(View.GONE);
                    break;
                case "Pending":
                default:
                    txtOrderStatus.setTextColor(Color.parseColor("#FFA500"));
                    btnStartPreparing.setVisibility(View.VISIBLE);
                    btnFinishOrder.setVisibility(View.GONE);
                    break;
            }

            // Clear previous items
            layoutOrderItems.removeAllViews();

            // Add items with quantities
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                for (Map<String, Object> itemMap : order.getItems()) {
                    String name = (String) itemMap.get("name");
                    int quantity = Order.getQuantityFromItem(itemMap);
                    double price = Order.getPriceFromItem(itemMap);

                    TextView itemView = new TextView(getContext());
                    itemView.setText(String.format("• %s × %d ($%.2f)", name, quantity, price * quantity));
                    itemView.setTextSize(14);
                    itemView.setTextColor(Color.BLACK);
                    itemView.setPadding(8, 4, 8, 4);
                    layoutOrderItems.addView(itemView);

                    Log.d(TAG, "Order item: " + name + " × " + quantity);
                }
            }

            if (order.getTimestamp() != null) {
                txtOrderTimestamp.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(order.getTimestamp()));
            } else {
                txtOrderTimestamp.setText("");
            }

            btnStartPreparing.setOnClickListener(v -> updateOrderStatus(order, "Preparing"));
            btnFinishOrder.setOnClickListener(v -> updateOrderStatus(order, "Ready"));
        }

        return convertView;
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getId() != null) {
            db.collection("orders").document(order.getId()).update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Order " + order.getId() + " updated to " + newStatus);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update order", e);
                    });
        }
    }
}