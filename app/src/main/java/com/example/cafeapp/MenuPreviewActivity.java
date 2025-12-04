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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
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

public class MenuPreviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private GridView gridViewPreview;
    private MenuItemAdapter adapter;
    private ArrayList<MenuItem> menuItems;
    private ArrayList<MenuItem> allMenuItems;
    private List<String> categories;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
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
        setContentView(R.layout.activity_menu_preview);

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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
        fabAddItem.setOnClickListener(v -> showAddEditDialog(null));

        // Setup search functionality
        setupSearch();

        // Load categories and create buttons dynamically
        loadCategoriesFromFirestore();
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

    private void showAddEditDialog(@Nullable MenuItem item) {
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
            builder.setTitle("Add Menu Item");
            currentImageBase64 = "";
        }

        btnSelectImage.setOnClickListener(v -> openImagePicker());

        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            String newCategory = edtNewCategory.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            String category;
            if (!newCategory.isEmpty()) {
                category = newCategory;
            } else if (spinnerCategory.getSelectedItem() != null) {
                category = spinnerCategory.getSelectedItem().toString();
            } else {
                Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (item != null) {
                updateMenuItem(item.getId(), name, description, price, category);
            } else {
                addMenuItem(name, description, price, category);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addMenuItem(String name, String description, double price, String category) {
        String id = db.collection("menuItems").document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", currentImageBase64);

        db.collection("menuItems").document(id).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    loadCategoriesFromFirestore(); // Reload to show new item
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMenuItem(String id, String name, String description, double price, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", currentImageBase64);

        db.collection("menuItems").document(id).update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                    loadMenuItems(selectedCategory);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
                });
    }

    public void deleteMenuItem(MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("menuItems").document(item.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                                loadMenuItems(selectedCategory);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void editMenuItem(MenuItem item) {
        showAddEditDialog(item);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK &&
                data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap compressedBitmap = compressBitmap(originalBitmap);
                dialogImageView.setImageBitmap(compressedBitmap);
                currentImageBase64 = bitmapToBase64Compressed(compressedBitmap);
            } catch (IOException e) {
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
            e.printStackTrace();
        });
    }

    private void createCategoryButtons() {
        categoryTabsContainer.removeAllViews();
        categoryButtons.clear();

        for (String category : categories) {
            Button categoryButton = new Button(this);

            String displayName = category.equals("All") ? "All Coffee" : category;
            categoryButton.setText(displayName);
            categoryButton.setTransformationMethod(null);

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

            // Remove elevation/shadow
            categoryButton.setStateListAnimator(null);
            categoryButton.setElevation(0);

            // Set initial style (unselected)
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
            btn.setStateListAnimator(null);  // Remove shadow
            btn.setElevation(0);              // Remove elevation
        }

        selectedButton.setBackgroundResource(R.drawable.category_selected_bg);
        selectedButton.setTextColor(Color.WHITE);
        selectedButton.setStateListAnimator(null);  // Remove shadow
        selectedButton.setElevation(0);              // Remove elevation
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void loadMenuItems(String categoryFilter) {
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

            adapter.notifyDataSetChanged();

            String searchText = searchBar.getText().toString();
            if (!searchText.isEmpty()) {
                filterMenuItems(searchText);
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_tables) {
            startActivity(new Intent(this, SetupTablesActivity.class));
        } else if (id == R.id.nav_preview) {
            // Already here
        } else if (id == R.id.nav_barista) {
            startActivity(new Intent(this, BaristaActivity.class));
        } else if (id == R.id.nav_manage_staff) {
            startActivity(new Intent(this, ManageStaffActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(this, Loginactivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}