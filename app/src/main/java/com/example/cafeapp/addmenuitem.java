//package com.example.cafeapp;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Base64;
//import android.widget.*;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set; // <-- FIXED
//import java.util.HashSet; // <-- FIXED
//
//public class addmenuitem extends AppCompatActivity {
//
//    private EditText edtName, edtCategory, edtDescription, edtPrice;
//    private Spinner spinnerCategory;
//    private ImageView imgMenu;
//    private Button btnSave, btnSelectImage;
//    private FirebaseFirestore db;
//    private ArrayList<String> categoryList;
//    private ArrayAdapter<String> adapter;
//
//    private String editItemId = null;
//    private String imageBase64 = "";
//
//    private static final int IMAGE_PICK_CODE = 1000;
//    private static final int MAX_WIDTH = 400;
//    private static final int MAX_HEIGHT = 400;
//    private static final int JPEG_QUALITY = 50;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_addmenuitem);
//
//        edtName = findViewById(R.id.editName);
//        edtCategory = findViewById(R.id.editNewCategory);
//        edtDescription = findViewById(R.id.editDescription);
//        edtPrice = findViewById(R.id.editPrice);
//        spinnerCategory = findViewById(R.id.spinnerCategory);
//        imgMenu = findViewById(R.id.imagePreview);
//        btnSave = findViewById(R.id.btnSave);
//        btnSelectImage = findViewById(R.id.btnSelectImage);
//
//        db = FirebaseFirestore.getInstance();
//
//        categoryList = new ArrayList<>();
//        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerCategory.setAdapter(adapter);
//
//        editItemId = getIntent().getStringExtra("editItemId");
//        if (editItemId != null) {
//            loadMenuItemData(editItemId);
//        } else {
//            loadCategoriesAndSetSelected(null);
//        }
//
//        btnSelectImage.setOnClickListener(v -> openImagePicker());
//        btnSave.setOnClickListener(v -> saveMenuItemWithValidation());
//    }
//
//    private void loadCategoriesAndSetSelected(@Nullable String selectedCategory) {
//        categoryList.clear();
//        db.collection("menuItems").get().addOnSuccessListener(querySnapshot -> {
//            Set<String> categories = new HashSet<>();
//            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//                String category = doc.getString("category");
//                if (category != null && !category.isEmpty()) {
//                    categories.add(category);
//                }
//            }
//            categoryList.addAll(categories);
//            adapter.notifyDataSetChanged();
//
//            if (selectedCategory != null && categoryList.contains(selectedCategory)) {
//                spinnerCategory.setSelection(categoryList.indexOf(selectedCategory));
//            }
//        });
//    }
//
//    private void loadMenuItemData(String itemId) {
//        db.collection("menuItems").document(itemId).get().addOnSuccessListener(doc -> {
//            if (doc.exists()) {
//                edtName.setText(doc.getString("name"));
//                edtDescription.setText(doc.getString("description"));
//                edtPrice.setText(String.valueOf(doc.getDouble("price")));
//                String category = doc.getString("category");
//                loadCategoriesAndSetSelected(category);
//                edtCategory.setText(category);
//
//                imageBase64 = doc.getString("imageBase64");
//                if (imageBase64 != null && !imageBase64.isEmpty()) {
//                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
//                    imgMenu.setImageBitmap(bitmap);
//                }
//            }
//        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show());
//    }
//
//    private void saveMenuItemWithValidation() {
//        String name = edtName.getText().toString().trim();
//        String description = edtDescription.getText().toString().trim();
//        String newCategory = edtCategory.getText().toString().trim();
//        double price;
//
//        try {
//            price = Double.parseDouble(edtPrice.getText().toString().trim());
//        } catch (NumberFormatException e) {
//            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String category;
//        if (!newCategory.isEmpty()) {
//            category = newCategory;
//        } else if (spinnerCategory.getSelectedItem() != null) {
//            category = spinnerCategory.getSelectedItem().toString();
//        } else {
//            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (name.isEmpty()) {
//            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        saveMenuItem(name, description, price, category);
//    }
//
//    private void saveMenuItem(String name, String description, double price, String category) {
//        Map<String, Object> data = new HashMap<>();
//        data.put("name", name);
//        data.put("description", description);
//        data.put("price", price);
//        data.put("category", category);
//        data.put("imageBase64", imageBase64);
//
//        if (editItemId != null) {
//            db.collection("menuItems").document(editItemId).update(data)
//                    .addOnSuccessListener(aVoid -> {
//                        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
//                        finish();
//                    })
//                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
//        } else {
//            String id = db.collection("menuItems").document().getId();
//            data.put("id", id);
//            db.collection("menuItems").document(id).set(data)
//                    .addOnSuccessListener(aVoid -> {
//                        Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
//                        finish();
//                    })
//                    .addOnFailureListener(e -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
//        }
//    }
//
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.setType("image/*");
//        startActivityForResult(intent, IMAGE_PICK_CODE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
//            try {
//                Uri imageUri = data.getData();
//                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                Bitmap compressedBitmap = compressBitmap(originalBitmap);
//                imgMenu.setImageBitmap(compressedBitmap);
//                imageBase64 = bitmapToBase64Compressed(compressedBitmap);
//            } catch (IOException e) {
//                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private Bitmap compressBitmap(Bitmap original) {
//        int width = original.getWidth();
//        int height = original.getHeight();
//        float scaleWidth = ((float) MAX_WIDTH) / width;
//        float scaleHeight = ((float) MAX_HEIGHT) / height;
//        float scaleFactor = Math.min(scaleWidth, scaleHeight);
//        if (scaleFactor > 1.0f) {
//            scaleFactor = 1.0f;
//        }
//        int newWidth = Math.round(width * scaleFactor);
//        int newHeight = Math.round(height * scaleFactor);
//        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
//    }
//
//    private String bitmapToBase64Compressed(Bitmap bitmap) {
//        if (bitmap == null) return "";
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
//        byte[] imageBytes = baos.toByteArray();
//        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
//    }
//}