package com.example.cafeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MenuForAdminActivity extends LogicAct {

    private static final String TAG = "MenuAdmin";

    private GridView gridViewPreview;
    private MenuItemAdapter adapter;
    private ArrayList<MenuItem> menuItems;
    private ArrayList<MenuItem> allMenuItems;
    private List<String> categories;
    private FirebaseFirestore db;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    private EditText searchBar;
    private LinearLayout categoryTabsContainer;
    private List<Button> categoryButtons;
    private String selectedCategory = "All";
    private Button fabAddItem;

    // Image handling
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 400;
    private static final int JPEG_QUALITY = 50;
    private String currentImageBase64 = "";
    private ImageView dialogImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_for_admin);
        Log.d(TAG, "onCreate called");

        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar/drawer in base activity (LogicAct)
        toolbar = findViewById(R.id.toolbar);
        setupDrawer();

        // Initialize views
        gridViewPreview = findViewById(R.id.gridViewPreview);
        searchBar = findViewById(R.id.searchBar);
        categoryTabsContainer = findViewById(R.id.categoryTabsContainer);
        fabAddItem = findViewById(R.id.fabAddItem);

        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        allMenuItems = new ArrayList<>();
        categoryButtons = new ArrayList<>();

        adapter = new MenuItemAdapter(this, menuItems, false); // Admin view
        gridViewPreview.setAdapter(adapter);

        // Setup FAB click
        fabAddItem.setOnClickListener(v -> {
            Log.d(TAG, "Add button clicked");
            showAddEditDialog(null);
        });

        // Setup search functionality
        setupSearch();

        // Load categories and create buttons dynamically
        loadCategoriesFromFirestore();
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenuItems(s.toString());
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
                boolean matchesSearch = item.getName() != null && item.getName().toLowerCase().contains(searchLower)
                        || (item.getDescription() != null &&
                        item.getDescription().toLowerCase().contains(searchLower));

                if (matchesCategory && matchesSearch) {
                    menuItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showAddEditDialog(@Nullable MenuItem item) {
        Log.d(TAG, "showAddEditDialog called, item: " + (item != null ? item.getName() : "null"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu_item, null);

        EditText edtName = dialogView.findViewById(R.id.edtName);
        EditText edtDescription = dialogView.findViewById(R.id.edtDescription);
        EditText edtPrice = dialogView.findViewById(R.id.edtPrice);
        EditText edtNewCategory = dialogView.findViewById(R.id.edtNewCategory);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        dialogImageView = dialogView.findViewById(R.id.imgPreview);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);

        // Setup category spinner
        ArrayList<String> categoryList = new ArrayList<>(categories);
        if (categoryList.contains("All")) {
            categoryList.remove("All");
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // If editing, populate fields
        if (item != null) {
            Log.d(TAG, "Editing item - ID: " + item.getId() + ", Name: " + item.getName());
            builder.setTitle("Edit Menu Item");
            edtName.setText(item.getName());
            edtDescription.setText(item.getDescription());
            edtPrice.setText(String.valueOf(item.getPrice()));

            if (categoryList.contains(item.getCategory())) {
                spinnerCategory.setSelection(categoryList.indexOf(item.getCategory()));
            }
            edtNewCategory.setText(item.getCategory());

            currentImageBase64 = item.getImageBase64();
            if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(currentImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                dialogImageView.setImageBitmap(bitmap);
            }
        } else {
            Log.d(TAG, "Adding new item");
            builder.setTitle("Add Menu Item");
            currentImageBase64 = "";
        }

        btnSelectImage.setOnClickListener(v -> openImagePicker());

        builder.setView(dialogView);
        builder.setPositiveButton("Save", null); // Set to null initially
        builder.setNegativeButton("Cancel", null);

        // Create and show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismiss
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#C67C4E"));
            positiveButton.setOnClickListener(v -> {
                Log.d(TAG, "Save button clicked");

                String name = edtName.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();
                String priceStr = edtPrice.getText().toString().trim();
                String newCategory = edtNewCategory.getText().toString().trim();

                Log.d(TAG, "Input - Name: " + name + ", Price: " + priceStr + ", NewCategory: " + newCategory);

                // Validation
                if (name.isEmpty()) {
                    Log.d(TAG, "Validation failed: Name is empty");
                    edtName.setError("Name is required");
                    edtName.requestFocus();
                    return;
                }

                if (priceStr.isEmpty()) {
                    Log.d(TAG, "Validation failed: Price is empty");
                    edtPrice.setError("Price is required");
                    edtPrice.requestFocus();
                    return;
                }

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        Log.d(TAG, "Validation failed: Price is negative");
                        edtPrice.setError("Price must be positive");
                        edtPrice.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Validation failed: Invalid price format");
                    edtPrice.setError("Invalid price");
                    edtPrice.requestFocus();
                    return;
                }

                String category;
                if (!newCategory.isEmpty()) {
                    category = newCategory;
                } else if (spinnerCategory.getSelectedItem() != null) {
                    category = spinnerCategory.getSelectedItem().toString();
                } else {
                    Log.d(TAG, "Validation failed: No category selected");
                    Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Validation passed. Final category: " + category);

                // All validation passed, proceed with save/update
                if (item != null) {
                    Log.d(TAG, "Calling updateMenuItem for ID: " + item.getId());
                    updateMenuItem(item.getId(), name, description, price, category);
                } else {
                    Log.d(TAG, "Calling addMenuItem");
                    addMenuItem(name, description, price, category);
                }

                // Dismiss dialog after successful validation
                dialog.dismiss();
            });
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#808080"));
        }
    }

    private void addMenuItem(String name, String description, double price, String category) {
        Log.d(TAG, "addMenuItem called");
        String id = db.collection("menuItems").document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", currentImageBase64);

        Log.d(TAG, "Starting Firestore add for ID: " + id);

        db.collection("menuItems").document(id).set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Add SUCCESS!");
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    loadCategoriesFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Add FAILED: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateMenuItem(String id, String name, String description, double price, String category) {
        Log.d(TAG, "updateMenuItem called");
        Log.d(TAG, "ID: " + id);
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Price: " + price);
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Image Base64 length: " + (currentImageBase64 != null ? currentImageBase64.length() : "null"));

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", currentImageBase64);

        Log.d(TAG, "Starting Firestore update...");

        db.collection("menuItems").document(id).update(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Update SUCCESS!");
                    Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                    loadCategoriesFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update FAILED: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public void deleteMenuItem(MenuItem item) {
        Log.d(TAG, "deleteMenuItem called for item: " + (item != null ? item.getName() : "null"));

        if (item == null) {
            Log.e(TAG, "Item is NULL!");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d(TAG, "Delete confirmed for ID: " + item.getId());
                    db.collection("menuItems").document(item.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Delete SUCCESS!");
                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                                loadCategoriesFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Delete FAILED: " + e.getMessage(), e);
                                Toast.makeText(this, "Failed to delete item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d(TAG, "Delete cancelled");
                })
                .show();
    }

    public void editMenuItem(MenuItem item) {
        Log.d(TAG, "editMenuItem called with item: " + (item != null ? item.getName() : "null"));
        if (item == null) {
            Log.e(TAG, "Item is NULL!");
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddEditDialog(item);
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker called");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK &&
                data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                Log.d(TAG, "Image selected: " + imageUri);

                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap compressedBitmap = compressBitmap(originalBitmap);
                dialogImageView.setImageBitmap(compressedBitmap);
                currentImageBase64 = bitmapToBase64Compressed(compressedBitmap);

                Log.d(TAG, "Image processed, Base64 length: " + currentImageBase64.length());
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image", e);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        float scaleWidth = ((float) MAX_WIDTH) / width;
        float scaleHeight = ((float) MAX_HEIGHT) / height;
        float scaleFactor = Math.min(scaleWidth, scaleHeight);
        if (scaleFactor > 1.0f) {
            scaleFactor = 1.0f;
        }
        int newWidth = Math.round(width * scaleFactor);
        int newHeight = Math.round(height * scaleFactor);
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private String bitmapToBase64Compressed(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void loadCategoriesFromFirestore() {
        Log.d(TAG, "loadCategoriesFromFirestore called");

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

            Log.d(TAG, "Categories loaded: " + categories);

            createCategoryButtons();
            loadMenuItems("All");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load categories", e);
        });
    }

    private void createCategoryButtons() {
        Log.d(TAG, "createCategoryButtons called");
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

            // Remove elevation/shadow
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
        Log.d(TAG, "Category selected: " + category);

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
        Log.d(TAG, "loadMenuItems called for category: " + categoryFilter);

        Query query;

        if ("All".equals(categoryFilter)) {
            query = db.collection("menuItems");
        } else {
            query = db.collection("menuItems").whereEqualTo("category", categoryFilter);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            allMenuItems.clear();
            menuItems.clear();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                MenuItem item = new MenuItem(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("description"),
                        doc.getDouble("price"),
                        doc.getString("category"),
                        doc.getString("imageBase64")
                );
                allMenuItems.add(item);
                menuItems.add(item);
            }

            Log.d(TAG, "Loaded " + menuItems.size() + " items");
            adapter.notifyDataSetChanged();

            String searchText = searchBar.getText().toString();
            if (!searchText.isEmpty()) {
                filterMenuItems(searchText);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load menu items", e);
        });
    }
}