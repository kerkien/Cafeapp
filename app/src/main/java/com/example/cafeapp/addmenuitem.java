package com.example.cafeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class addmenuitem extends AppCompatActivity {

    private EditText edtName, edtCategory, edtDescription, edtPrice;
    private Spinner spinnerCategory;
    private ImageView imgMenu;
    private Button btnSave, btnSelectImage;

    private FirebaseFirestore db;
    private ArrayList<String> categoryList;
    private ArrayAdapter<String> adapter;

    private String editItemId = null;
    private String imageBase64 = "";

    private static final int IMAGE_PICK_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addmenuitem);

        // Initialize UI components
        edtName = findViewById(R.id.editName);
        edtCategory = findViewById(R.id.editNewCategory); // Optional new category
        edtDescription = findViewById(R.id.editDescription);
        edtPrice = findViewById(R.id.editPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgMenu = findViewById(R.id.imagePreview);
        btnSave = findViewById(R.id.btnSave);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        // Firestore setup
        db = FirebaseFirestore.getInstance();

        // Spinner setup
        categoryList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Editing an existing item?
        editItemId = getIntent().getStringExtra("editItemId");
        if (editItemId != null) {
            loadMenuItemData(editItemId);
        } else {
            loadCategoriesAndSetSelected(null);
        }

        // Image selection
        btnSelectImage.setOnClickListener(v -> openImagePicker());

        // Save button logic
        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String newCategory = edtCategory.getText().toString().trim();
            double price;

            // Validate price
            try {
                price = Double.parseDouble(edtPrice.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine category source (spinner or new)
            String category;
            if (!newCategory.isEmpty()) {
                category = newCategory;
            } else if (spinnerCategory.getSelectedItem() != null) {
                category = spinnerCategory.getSelectedItem().toString();
            } else {
                Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            saveMenuItem(name, description, price, category);
        });
    }

    /** Load all unique categories and optionally set a pre-selected one **/
    private void loadCategoriesAndSetSelected(@Nullable String selectedCategory) {
        categoryList.clear();
        db.collection("menuItems")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String category = doc.getString("category");
                        if (category != null && !categoryList.contains(category)) {
                            categoryList.add(category);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Pre-select spinner item if editing
                    if (selectedCategory != null && categoryList.contains(selectedCategory)) {
                        spinnerCategory.setSelection(categoryList.indexOf(selectedCategory));
                    }
                });
    }

    /** Load existing menu item data for editing **/
    private void loadMenuItemData(String itemId) {
        db.collection("menuItems").document(itemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String description = doc.getString("description");
                        Double price = doc.getDouble("price");
                        String category = doc.getString("category");
                        imageBase64 = doc.getString("imageBase64");

                        // Populate fields
                        edtName.setText(name);
                        edtDescription.setText(description);
                        edtPrice.setText(price != null ? String.valueOf(price) : "");

                        // Load spinner + set selected category
                        loadCategoriesAndSetSelected(category);

                        // Display image
                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            imgMenu.setImageBitmap(base64ToBitmap(imageBase64));
                        }

                        // Also fill optional category field
                        if (category != null) {
                            edtCategory.setText(category);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show());
    }

    /** Save or update a menu item **/
    private void saveMenuItem(String name, String description, double price, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", imageBase64);

        if (editItemId != null) {
            // Update existing
            db.collection("menuItems").document(editItemId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        } else {
            // Add new
            String id = db.collection("menuItems").document().getId();
            data.put("id", id);
            db.collection("menuItems").document(id)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
        }
    }

    /** Open image picker **/
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    /** Handle selected image **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                imgMenu.setImageBitmap(bitmap);
                imageBase64 = bitmapToBase64(bitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Convert Bitmap to Base64 **/
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /** Convert Base64 to Bitmap **/
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}
