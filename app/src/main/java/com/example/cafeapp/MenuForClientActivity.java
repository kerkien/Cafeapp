package com.example.cafeapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

public class MenuForClientActivity extends AppCompatActivity {

    private static final String TAG = "MenuClient";

    private GridView gridViewMenu;
    private ArrayList<MenuItem> menuItems;
    private ArrayList<MenuItem> allMenuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;
    private Button btnPlaceOrder;
    private TextView txtTableNumber;
    private TextView txtCartSummary;
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
        txtCartSummary = findViewById(R.id.txtCartSummary);
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

        setupSearch();
        loadCategoriesFromFirestore();

        gridViewMenu.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {}

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateCartSummary();
            }
        });

        btnPlaceOrder.setOnClickListener(v -> {
            // FIXED - Get CartItem list instead of MenuItem list
            List<CartItem> cartItems = adapter.getCartItems();
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Please add at least one item to cart.", Toast.LENGTH_SHORT).show();
                return;
            }
            placeOrder(cartItems);
        });
    }

    private void updateCartSummary() {
        int itemCount = adapter.getCartItemCount();
        double total = adapter.getCartTotal();

        if (itemCount > 0) {
            txtCartSummary.setText(String.format("%d items - $%.2f", itemCount, total));
            txtCartSummary.setVisibility(TextView.VISIBLE);
        } else {
            txtCartSummary.setVisibility(TextView.GONE);
        }
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
            public void afterTextChanged(Editable s) {
                updateCartSummary();
            }
        });
    }

    private void filterMenuItems(String searchText) {
        menuItems.clear();

        if (searchText.isEmpty()) {
            if (selectedCategory.equals("All")) {
                menuItems.addAll(allMenuItems);
            } else {
                for (MenuItem item : allMenuItems) {
                    if (item.getCategory() != null && item.getCategory().equals(selectedCategory)) {
                        menuItems.add(item);
                    }
                }
            }
        } else {
            String searchLower = searchText.toLowerCase();
            for (MenuItem item : allMenuItems) {
                boolean matchesCategory = selectedCategory.equals("All") ||
                        (item.getCategory() != null && item.getCategory().equals(selectedCategory));
                boolean matchesSearch = (item.getName() != null && item.getName().toLowerCase().contains(searchLower)) ||
                        (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchLower));

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

            createCategoryButtons();
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

            String displayName = category.equals("All") ? "All" : category;
            categoryButton.setText(displayName);
            categoryButton.setTransformationMethod(null);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, dpToPx(8), 0);
            categoryButton.setLayoutParams(params);

            int padding = dpToPx(20);
            int paddingVertical = dpToPx(10);
            categoryButton.setPadding(padding, paddingVertical, padding, paddingVertical);

            categoryButton.setStateListAnimator(null);
            categoryButton.setElevation(0);
            categoryButton.setBackgroundResource(R.drawable.category_unselected_bg);
            categoryButton.setTextColor(Color.parseColor("#808080"));

            categoryButton.setOnClickListener(v -> {
                selectCategory(categoryButton, category);
                loadMenuItems(category);
            });

            categoryTabsContainer.addView(categoryButton);
            categoryButtons.add(categoryButton);
        }

        if (!categoryButtons.isEmpty()) {
            selectCategory(categoryButtons.get(0), "All");
        }
    }

    private void selectCategory(Button selectedButton, String category) {
        selectedCategory = category;

        for (Button btn : categoryButtons) {
            btn.setBackgroundResource(R.drawable.category_unselected_bg);
            btn.setTextColor(Color.parseColor("#808080"));
            btn.setStateListAnimator(null);
            btn.setElevation(0);
        }

        selectedButton.setBackgroundResource(R.drawable.category_selected_bg);
        selectedButton.setTextColor(Color.WHITE);
        selectedButton.setStateListAnimator(null);
        selectedButton.setElevation(0);
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

            String searchText = searchBar.getText().toString();
            if (!searchText.isEmpty()) {
                filterMenuItems(searchText);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show();
        });
    }

    private void placeOrder(List<CartItem> cartItems) {
        if (tableId == null) {
            Toast.makeText(this, "Invalid table ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            MenuItem item = cartItem.getMenuItem();
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("id", item.getId());
            orderItem.put("name", item.getName());
            orderItem.put("price", item.getPrice());
            orderItem.put("quantity", cartItem.getQuantity()); // CRITICAL - Save quantity
            orderItems.add(orderItem);

            Log.d(TAG, "Adding to order: " + item.getName() + " x" + cartItem.getQuantity());
        }

        Log.d(TAG, "Placing order with " + orderItems.size() + " items");

        Order order = new Order(tableId, orderItems);
        db.collection("orders").add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MenuForClientActivity.this, OrderTrackingActivity.class);
                    intent.putExtra("orderId", documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to place order", e);
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}