package com.example.cafeapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MenuForClientActivity extends LogicAct {

    private GridView gridViewMenu;
    private ArrayList<MenuItem> menuItems;
    private ArrayList<MenuItem> allMenuItems; // Store all items for search
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;
    private Button btnPlaceOrder;
    private TextView txtTableNumber;
    private EditText searchBar;
    private String tableId;
    private List<String> categories;
    private LinearLayout categoryTabsContainer;
    private List<Button> categoryButtons;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_for_client);

        gridViewMenu = findViewById(R.id.gridViewMenu);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        txtTableNumber = findViewById(R.id.txtTableNumber);
        searchBar = findViewById(R.id.searchBar);
        categoryTabsContainer = findViewById(R.id.categoryTabsContainer);

        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        allMenuItems = new ArrayList<>();
        categoryButtons = new ArrayList<>();

        adapter = new MenuItemAdapter(this, menuItems, true);
        gridViewMenu.setAdapter(adapter);

        Uri data = getIntent().getData();
        if (data != null) {
            tableId = data.getQueryParameter("table");
        }

        if (tableId != null) {
            txtTableNumber.setText("Table: " + tableId);
        } else {
            txtTableNumber.setText("Table not found");
        }

        // Setup search functionality
        setupSearch();

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

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenuItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMenuItems(String searchText) {
        menuItems.clear();

        if (searchText.isEmpty()) {
            // If search is empty, show items based on selected category
            if (selectedCategory.equals("All")) {
                menuItems.addAll(allMenuItems);
            } else {
                for (MenuItem item : allMenuItems) {
                    if (item.getCategory().equals(selectedCategory)) {
                        menuItems.add(item);
                    }
                }
            }
        } else {
            // Filter by search text and category
            String searchLower = searchText.toLowerCase();
            for (MenuItem item : allMenuItems) {
                boolean matchesCategory = selectedCategory.equals("All") ||
                        item.getCategory().equals(selectedCategory);
                boolean matchesSearch = item.getName().toLowerCase().contains(searchLower) ||
                        (item.getDescription() != null &&
                                item.getDescription().toLowerCase().contains(searchLower));

                if (matchesCategory && matchesSearch) {
                    menuItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
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

            // Create category buttons dynamically
            createCategoryButtons();

            // Load all items initially
            loadMenuItems("All");
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show();
        });
    }

    private void createCategoryButtons() {
        categoryTabsContainer.removeAllViews();
        categoryButtons.clear();

        for (String category : categories) {
            Button categoryButton = new Button(this);

            // Set button text
            String displayName = category.equals("All") ? "All" : category;
            categoryButton.setText(displayName);
            categoryButton.setTransformationMethod(null); // Prevents all caps

            // Set button layout params
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, dpToPx(8), 0);
            categoryButton.setLayoutParams(params);

            // Set padding
            int padding = dpToPx(20);
            int paddingVertical = dpToPx(10);
            categoryButton.setPadding(padding, paddingVertical, padding, paddingVertical);

            // Set initial style (unselected)
            categoryButton.setBackgroundResource(R.drawable.category_unselected_bg);
            categoryButton.setTextColor(Color.parseColor("#808080"));

            // Set click listener
            categoryButton.setOnClickListener(v -> {
                selectCategory(categoryButton, category);
                loadMenuItems(category);
            });

            categoryTabsContainer.addView(categoryButton);
            categoryButtons.add(categoryButton);
        }

        // Select first button (All) by default
        if (!categoryButtons.isEmpty()) {
            selectCategory(categoryButtons.get(0), "All");
        }
    }

    private void selectCategory(Button selectedButton, String category) {
        selectedCategory = category;

        // Reset all buttons to unselected state
        for (Button btn : categoryButtons) {
            btn.setBackgroundResource(R.drawable.category_unselected_bg);
            btn.setTextColor(Color.parseColor("#808080"));
        }

        // Set selected button
        selectedButton.setBackgroundResource(R.drawable.category_selected_bg);
        selectedButton.setTextColor(Color.WHITE);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void loadMenuItems(String categoryFilter) {
        Query query = "All".equals(categoryFilter)
                ? db.collection("menuItems")
                : db.collection("menuItems").whereEqualTo("category", categoryFilter);

        query.get().addOnSuccessListener(querySnapshot -> {
            allMenuItems.clear();
            menuItems.clear();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Double price = doc.getDouble("price");
                if (price == null) price = 0.0;

                MenuItem item = new MenuItem(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("description"),
                        price,
                        doc.getString("category"),
                        doc.getString("imageBase64")
                );
                allMenuItems.add(item);
                menuItems.add(item);
            }

            adapter.notifyDataSetChanged();

            // Reapply search filter if there's text in search bar
            String searchText = searchBar.getText().toString();
            if (!searchText.isEmpty()) {
                filterMenuItems(searchText);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show();
        });
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
                    Intent intent = new Intent(MenuForClientActivity.this, OrderTrackingActivity.class);
                    intent.putExtra("orderId", documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}