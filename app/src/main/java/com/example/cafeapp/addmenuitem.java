package com.example.cafeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    // Maximum image dimensions and quality
    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 400;
    private static final int JPEG_QUALITY = 50; // 50% quality
    private static final int MAX_SIZE_BYTES = 100000; // ~100KB max

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addmenuitem);

        // Initialize UI components
        edtName = findViewById(R.id.editName);
        edtCategory = findViewById(R.id.editNewCategory);
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

            // Determine category source
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

                    if (selectedCategory != null && categoryList.contains(selectedCategory)) {
                        spinnerCategory.setSelection(categoryList.indexOf(selectedCategory));
                    }
                });
    }

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

                        edtName.setText(name);
                        edtDescription.setText(description);
                        edtPrice.setText(price != null ? String.valueOf(price) : "");

                        loadCategoriesAndSetSelected(category);

                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            imgMenu.setImageBitmap(base64ToBitmap(imageBase64));
                        }

                        if (category != null) {
                            edtCategory.setText(category);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show());
    }

    private void saveMenuItem(String name, String description, double price, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("price", price);
        data.put("category", category);
        data.put("imageBase64", imageBase64);

        if (editItemId != null) {
            db.collection("menuItems").document(editItemId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        } else {
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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Compress the bitmap
                Bitmap compressedBitmap = compressBitmap(originalBitmap);

                // Display compressed image
                imgMenu.setImageBitmap(compressedBitmap);

                // Convert to base64
                imageBase64 = bitmapToBase64Compressed(compressedBitmap);

                // Log the size for debugging
                int sizeInBytes = imageBase64.length();
                int sizeInKB = sizeInBytes / 1024;
                Log.d("ImageSize", "Compressed image size: " + sizeInKB + " KB");

                if (sizeInBytes > MAX_SIZE_BYTES) {
                    Toast.makeText(this, "Warning: Image is " + sizeInKB + "KB (recommended <100KB)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Image compressed to " + sizeInKB + "KB", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                Log.e("ImageError", "Error loading image", e);
            }
        }
    }

    /**
     * Compress bitmap to reduce size significantly
     */
    private Bitmap compressBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate scaling factor
        float scaleWidth = ((float) MAX_WIDTH) / width;
        float scaleHeight = ((float) MAX_HEIGHT) / height;
        float scaleFactor = Math.min(scaleWidth, scaleHeight);

        // Don't scale up, only scale down
        if (scaleFactor > 1.0f) {
            scaleFactor = 1.0f;
        }

        int newWidth = Math.round(width * scaleFactor);
        int newHeight = Math.round(height * scaleFactor);

        // Resize the bitmap
        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);

        // Recycle original if it's different
        if (resized != original) {
            original.recycle();
        }

        return resized;
    }

    /**
     * Convert Bitmap to compressed Base64 string
     * Uses JPEG compression for smaller size
     */
    private String bitmapToBase64Compressed(Bitmap bitmap) {
        if (bitmap == null) return "";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use JPEG with quality compression (50% quality)
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);

        byte[] imageBytes = baos.toByteArray();

        // Log size
        Log.d("ImageCompression", "Final size: " + (imageBytes.length / 1024) + " KB");

        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Legacy method - kept for backward compatibility
     * But now uses compression
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Use JPEG instead of PNG for smaller size
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Convert Base64 to Bitmap
     */
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            Log.e("Base64Decode", "Error decoding base64", e);
            return null;
        }
    }
}