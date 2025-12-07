package com.example.cafeapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SetupTablesActivity extends LogicAct {

    private EditText edtTableCount;
    private Button btnGenerate;
    private LinearLayout layoutQrContainer;
    private FirebaseFirestore db;

    private int currentMaxTableNumber = 0;
    private List<TableData> existingTables = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Bitmap bitmapToSave;
    private String fileNameToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_tables);

        // Setup drawer from LogicAct
        setupDrawer();

        edtTableCount = findViewById(R.id.edtTableCount);
        btnGenerate = findViewById(R.id.btnGenerate);
        layoutQrContainer = findViewById(R.id.layoutQrContainer);

        db = FirebaseFirestore.getInstance();

        loadExistingTables();

        btnGenerate.setOnClickListener(v -> generateTableQRCodes());
    }

    private void loadExistingTables() {
        db.collection("tables")
                .orderBy("tableNumber")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    existingTables.clear();
                    layoutQrContainer.removeAllViews();
                    currentMaxTableNumber = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TableData table = doc.toObject(TableData.class);
                        existingTables.add(table);

                        if (table.tableNumber > currentMaxTableNumber) {
                            currentMaxTableNumber = table.tableNumber;
                        }
                        displayTableCard(table);
                    }
                    if (currentMaxTableNumber > 0) {
                        edtTableCount.setHint("Next table will start from " + (currentMaxTableNumber + 1));
                    }
                })
                .addOnFailureListener(e -> {
                    msg("Failed to load tables: " + e.getMessage());
                });
    }

    private void displayTableCard(TableData table) {
        View cardView = getLayoutInflater().inflate(R.layout.table_card_item, layoutQrContainer, false);
        TextView txtTableTitle = cardView.findViewById(R.id.txtTableTitle);
        ImageView imgQrCode = cardView.findViewById(R.id.imgQrCode);
        TextView txtQrLink = cardView.findViewById(R.id.txtQrLink);
        Button btnUpdate = cardView.findViewById(R.id.btnUpdate);
        Button btnDelete = cardView.findViewById(R.id.btnDelete);
        Button btnDownload = cardView.findViewById(R.id.btnDownload);

        txtTableTitle.setText("Table " + table.tableNumber);
        txtQrLink.setText(table.qrLink);

        Bitmap qrBitmap = generateQRCode(table.qrLink);
        if (qrBitmap != null) {
            imgQrCode.setImageBitmap(qrBitmap);
        }

        btnUpdate.setOnClickListener(v -> showUpdateDialog(table));
        btnDelete.setOnClickListener(v -> showDeleteConfirmation(table));
        btnDownload.setOnClickListener(v -> {
            if (qrBitmap != null) {
                handleDownloadRequest(qrBitmap, "Table_" + table.tableNumber + "_QR.png");
            } else {
                msg("QR Code not available");
            }
        });

        layoutQrContainer.addView(cardView);
    }

    private void generateTableQRCodes() {
        String input = edtTableCount.getText().toString().trim();
        if (input.isEmpty()) {
            msg("Enter number of tables");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            msg("Invalid number");
            return;
        }

        if (count <= 0 || count > 100) {
            msg("Please enter a number between 1 and 100");
            return;
        }

        int startNumber = currentMaxTableNumber + 1;

        for (int i = 0; i < count; i++) {
            int tableNumber = startNumber + i;
            String tableId = "Table_" + tableNumber;
            String qrContent = "https://yourapp.com/menu?table=" + tableNumber;

            TableData newTable = new TableData(tableId, tableNumber, qrContent);

            db.collection("tables").document(tableId).set(newTable)
                    .addOnFailureListener(e -> {
                        msg("Failed to create table " + tableNumber);
                    });
        }

        msg(count + " table(s) created successfully");
        edtTableCount.setText("");

        // Reload after a short delay to allow Firestore to update
        layoutQrContainer.postDelayed(this::loadExistingTables, 500);
    }

    private void showUpdateDialog(TableData table) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_table, null);

        EditText edtTableNumber = dialogView.findViewById(R.id.edtTableNumber);
        EditText edtQrLink = dialogView.findViewById(R.id.edtQrLink);

        edtTableNumber.setText(String.valueOf(table.tableNumber));
        edtQrLink.setText(table.qrLink);

        builder.setTitle("Update Table " + table.tableNumber)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newTableNumberStr = edtTableNumber.getText().toString().trim();
                    String newQrLink = edtQrLink.getText().toString().trim();

                    if (newTableNumberStr.isEmpty() || newQrLink.isEmpty()) {
                        msg("All fields are required");
                        return;
                    }

                    try {
                        int newTableNumber = Integer.parseInt(newTableNumberStr);
                        updateTable(table, newTableNumber, newQrLink);
                    } catch (NumberFormatException e) {
                        msg("Invalid table number");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTable(TableData oldTable, int newTableNumber, String newQrLink) {
        String newTableId = "Table_" + newTableNumber;

        // If table number changed, we need to delete old and create new
        if (oldTable.tableNumber != newTableNumber) {
            TableData newTable = new TableData(newTableId, newTableNumber, newQrLink);

            // Delete old table
            db.collection("tables").document(oldTable.id).delete()
                    .addOnSuccessListener(aVoid -> {
                        // Create new table
                        db.collection("tables").document(newTableId).set(newTable)
                                .addOnSuccessListener(aVoid2 -> {
                                    msg("Table updated successfully");
                                    loadExistingTables();
                                })
                                .addOnFailureListener(e -> {
                                    msg("Failed to create new table");
                                });
                    })
                    .addOnFailureListener(e -> {
                        msg("Failed to delete old table");
                    });
        } else {
            // Just update the QR link
            db.collection("tables").document(oldTable.id)
                    .update("qrLink", newQrLink)
                    .addOnSuccessListener(aVoid -> {
                        msg("QR link updated successfully");
                        loadExistingTables();
                    })
                    .addOnFailureListener(e -> {
                        msg("Failed to update QR link");
                    });
        }
    }

    private void showDeleteConfirmation(TableData table) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Table")
                .setMessage("Are you sure you want to delete Table " + table.tableNumber + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTable(table))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTable(TableData table) {
        db.collection("tables").document(table.id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    msg("Table " + table.tableNumber + " deleted");
                    loadExistingTables();
                })
                .addOnFailureListener(e -> {
                    msg("Failed to delete table: " + e.getMessage());
                });
    }

    private void handleDownloadRequest(Bitmap bitmap, String fileName) {
        // Check permission for Android 10 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                bitmapToSave = bitmap;
                fileNameToSave = fileName;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Permission granted or Android 10+, proceed with download
        saveQRCodeToGallery(bitmap, fileName);
    }

    private void saveQRCodeToGallery(Bitmap bitmap, String fileName) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 and above - use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CafeQR");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    fos = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) {
                        fos.close();
                    }
                    msg("QR Code saved to Gallery");
                } else {
                    msg("Failed to create file");
                }
            } else {
                // Below Android 10 - use legacy method
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString() + "/CafeQR";
                java.io.File file = new java.io.File(imagesDir);
                if (!file.exists()) {
                    file.mkdirs();
                }

                java.io.File imageFile = new java.io.File(imagesDir, fileName);
                fos = new java.io.FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                // Notify gallery
                MediaStore.Images.Media.insertImage(getContentResolver(),
                        imageFile.getAbsolutePath(), fileName, "QR Code");

                msg("QR Code saved to Gallery");
            }
        } catch (IOException e) {
            e.printStackTrace();
            msg("Failed to save QR Code: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bitmapToSave != null && fileNameToSave != null) {
                    saveQRCodeToGallery(bitmapToSave, fileNameToSave);
                    bitmapToSave = null;
                    fileNameToSave = null;
                }
            } else {
                msg("Permission denied. Cannot save QR Code.");
            }
        }
    }

    private Bitmap generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int size = 512;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class TableData {
        public String id;
        public int tableNumber;
        public String qrLink;

        public TableData() {}

        public TableData(String id, int tableNumber, String qrLink) {
            this.id = id;
            this.tableNumber = tableNumber;
            this.qrLink = qrLink;
        }
    }
}