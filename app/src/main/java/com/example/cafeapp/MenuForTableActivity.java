package com.example.cafeapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MenuForTableActivity extends AppCompatActivity {

    private ListView listViewMenu;
    private Spinner spinnerCategory;
    private ArrayList<MenuItem> menuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;
    private Button btnPlaceOrder;
    private TextView txtTableNumber;
    private String tableId;
    private List<String> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_for_table);

        listViewMenu = findViewById(R.id.listViewMenu);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        txtTableNumber = findViewById(R.id.txtTableNumber);
        spinnerCategory = findViewById(R.id.spinnerCategory_table);
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItems, true);
        listViewMenu.setAdapter(adapter);

        Uri data = getIntent().getData();
        if (data != null) {
            tableId = data.getQueryParameter("table");
        }

        if (tableId != null) {
            txtTableNumber.setText("Table: " + tableId);
        } else {
            txtTableNumber.setText("Table not found");
        }

        loadCategoriesFromFirestore();

        btnPlaceOrder.setOnClickListener(v -> {
            List<MenuItem> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please select at least one item.", Toast.LENGTH_SHORT).show();
                return;
            }
            placeOrder(selectedItems);
        });
    }

    private void loadCategoriesFromFirestore() {
        db.collection("menuItems").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Set<String> categorySet = new HashSet<>();
            categorySet.add("All");
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String category = doc.getString("category");
                if (category != null && !category.trim().isEmpty()) {
                    categorySet.add(category.trim());
                }
            }
            categories = new ArrayList<>(categorySet);
            categories.sort((a, b) -> a.equals("All") ? -1 : b.equals("All") ? 1 : a.compareToIgnoreCase(b));

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(spinnerAdapter);

            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    loadMenuItems(parent.getItemAtPosition(position).toString());
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            loadMenuItems("All");
        });
    }

    private void loadMenuItems(String categoryFilter) {
        Query query = "All".equals(categoryFilter)
                ? db.collection("menuItems")
                : db.collection("menuItems").whereEqualTo("category", categoryFilter);

        query.get().addOnSuccessListener(querySnapshot -> {
            menuItems.clear();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Double price = doc.getDouble("price");
                if (price == null) price = 0.0;
                menuItems.add(new MenuItem(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("description"),
                        price,
                        doc.getString("category"),
                        doc.getString("imageBase64")
                ));
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show());
    }

    private void placeOrder(List<MenuItem> items) {
        if (tableId == null) {
            Toast.makeText(this, "Invalid table ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (MenuItem item : items) {
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("id", item.getId());
            orderItem.put("name", item.getName());
            orderItem.put("price", item.getPrice());
            orderItems.add(orderItem);
        }

        Order order = new Order(tableId, orderItems);
        db.collection("orders").add(order)
                .addOnSuccessListener(documentReference -> {
                    Intent intent = new Intent(MenuForTableActivity.this, OrderTrackingActivity.class);
                    intent.putExtra("orderId", documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}