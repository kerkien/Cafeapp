package com.example.cafeapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;

public class SetupTablesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText edtTableCount;
    private Button btnGenerate;
    private LinearLayout layoutQrContainer;
    private FirebaseFirestore db;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private int currentMaxTableNumber = 0;
    private List<TableData> existingTables = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_tables);

        // Setup toolbar and drawer
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize views
        edtTableCount = findViewById(R.id.edtTableCount);
        btnGenerate = findViewById(R.id.btnGenerate);
        layoutQrContainer = findViewById(R.id.layoutQrContainer);

        db = FirebaseFirestore.getInstance();

        // Load existing tables first
        loadExistingTables();

        btnGenerate.setOnClickListener(v -> generateTableQRCodes());
    }

    /**
     * Load all existing tables from Firestore
     */
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

                        // Track the highest table number
                        if (table.tableNumber > currentMaxTableNumber) {
                            currentMaxTableNumber = table.tableNumber;
                        }

                        // Display the table
                        displayTableCard(table);
                    }

                    // Update hint text
                    if (currentMaxTableNumber > 0) {
                        edtTableCount.setHint("Next table will start from " + (currentMaxTableNumber + 1));
                        Toast.makeText(this, "Loaded " + existingTables.size() + " tables", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading tables", e);
                    Toast.makeText(this, "Failed to load tables", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Display a table card with QR code and action buttons
     */
    private void displayTableCard(TableData table) {
        // Create card container
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(24, 24, 24, 24);
        cardLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        cardLayout.setLayoutParams(cardParams);

        // Table title
        TextView txtTable = new TextView(this);
        txtTable.setText("Table " + table.tableNumber);
        txtTable.setTextSize(20);
        txtTable.setTextColor(Color.BLACK);
        txtTable.setGravity(Gravity.CENTER);
        txtTable.setPadding(0, 0, 0, 16);
        cardLayout.addView(txtTable);

        // QR Code
        Bitmap qrBitmap = generateQRCode(table.qrLink);
        if (qrBitmap != null) {
            ImageView qrImage = new ImageView(this);
            qrImage.setImageBitmap(qrBitmap);
            LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
                    400, 400
            );
            qrParams.gravity = Gravity.CENTER;
            qrImage.setLayoutParams(qrParams);
            qrImage.setPadding(0, 0, 0, 16);
            cardLayout.addView(qrImage);
        }

        // QR Link text
        TextView txtLink = new TextView(this);
        txtLink.setText(table.qrLink);
        txtLink.setTextSize(12);
        txtLink.setTextColor(Color.GRAY);
        txtLink.setGravity(Gravity.CENTER);
        txtLink.setPadding(8, 8, 8, 16);
        cardLayout.addView(txtLink);

        // Button container
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayout.setLayoutParams(buttonContainerParams);

        // Update button
        Button btnUpdate = new Button(this);
        btnUpdate.setText("Update");
        btnUpdate.setBackgroundColor(Color.parseColor("#4CAF50"));
        btnUpdate.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        updateParams.setMargins(0, 0, 8, 0);
        btnUpdate.setLayoutParams(updateParams);
        btnUpdate.setOnClickListener(v -> showUpdateDialog(table));
        buttonLayout.addView(btnUpdate);

        // Delete button
        Button btnDelete = new Button(this);
        btnDelete.setText("Delete");
        btnDelete.setBackgroundColor(Color.parseColor("#F44336"));
        btnDelete.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        deleteParams.setMargins(8, 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> showDeleteConfirmation(table));
        buttonLayout.addView(btnDelete);

        cardLayout.addView(buttonLayout);
        layoutQrContainer.addView(cardLayout);
    }

    /**
     * Generate new table QR codes
     */
    private void generateTableQRCodes() {
        String input = edtTableCount.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter number of tables", Toast.LENGTH_SHORT).show();
            return;
        }

        int count;
        try {
            count = Integer.parseInt(input);
            if (count <= 0) {
                Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start from next available table number
        int startNumber = currentMaxTableNumber + 1;

        Toast.makeText(this, "Generating tables " + startNumber + " to " + (startNumber + count - 1), Toast.LENGTH_SHORT).show();

        for (int i = 0; i < count; i++) {
            int tableNumber = startNumber + i;
            String tableId = "Table_" + tableNumber;
            String qrContent = "https://yourapp.com/menu?table=" + tableNumber;

            TableData newTable = new TableData(tableId, tableNumber, qrContent);

            // Save to Firestore
            db.collection("tables").document(tableId)
                    .set(newTable)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Table saved: " + newTable.id);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error saving table", e);
                        Toast.makeText(this, "Error saving table: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Reload tables after generation
        Toast.makeText(this, count + " tables generated!", Toast.LENGTH_SHORT).show();
        edtTableCount.setText("");

        // Reload after a short delay to allow Firestore to update
        layoutQrContainer.postDelayed(() -> loadExistingTables(), 1000);
    }

    /**
     * Show dialog to update table information
     */
    private void showUpdateDialog(TableData table) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Table " + table.tableNumber);

        // Create a container layout for multiple inputs
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Table Number input
        TextView lblTableNumber = new TextView(this);
        lblTableNumber.setText("Table Number:");
        lblTableNumber.setTextSize(14);
        lblTableNumber.setPadding(0, 0, 0, 8);
        layout.addView(lblTableNumber);

        final EditText inputTableNumber = new EditText(this);
        inputTableNumber.setText(String.valueOf(table.tableNumber));
        inputTableNumber.setHint("Enter table number");
        inputTableNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputTableNumber.setPadding(20, 20, 20, 20);
        layout.addView(inputTableNumber);

        // Add spacing
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 30));
        layout.addView(spacer);

        // QR Link input
        TextView lblQrLink = new TextView(this);
        lblQrLink.setText("QR Code Link:");
        lblQrLink.setTextSize(14);
        lblQrLink.setPadding(0, 0, 0, 8);
        layout.addView(lblQrLink);

        final EditText inputQrLink = new EditText(this);
        inputQrLink.setText(table.qrLink);
        inputQrLink.setHint("Enter QR link");
        inputQrLink.setPadding(20, 20, 20, 20);
        layout.addView(inputQrLink);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newTableNumberStr = inputTableNumber.getText().toString().trim();
            String newLink = inputQrLink.getText().toString().trim();

            if (newTableNumberStr.isEmpty()) {
                Toast.makeText(this, "Table number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newLink.isEmpty()) {
                Toast.makeText(this, "QR link cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int newTableNumber = Integer.parseInt(newTableNumberStr);
                if (newTableNumber <= 0) {
                    Toast.makeText(this, "Table number must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateTable(table, newTableNumber, newLink);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid table number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Update table in Firestore with new number and link
     */
    private void updateTable(TableData table, int newTableNumber, String newLink) {
        String oldId = table.id;
        String newId = "Table_" + newTableNumber;

        // Check if table number changed
        if (table.tableNumber != newTableNumber) {
            // Check if new table number already exists
            db.collection("tables").document(newId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && !oldId.equals(newId)) {
                            Toast.makeText(this, "Table " + newTableNumber + " already exists!", Toast.LENGTH_LONG).show();
                        } else {
                            // Safe to update - delete old and create new
                            performTableUpdate(oldId, newId, newTableNumber, newLink);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error checking table: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Only link changed, simple update
            table.qrLink = newLink;
            db.collection("tables").document(oldId)
                    .set(table)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Table updated successfully", Toast.LENGTH_SHORT).show();
                        loadExistingTables();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error updating table", e);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Perform table update when table number changes
     */
    private void performTableUpdate(String oldId, String newId, int newTableNumber, String newLink) {
        // Create new table document
        TableData newTable = new TableData(newId, newTableNumber, newLink);

        db.collection("tables").document(newId)
                .set(newTable)
                .addOnSuccessListener(aVoid -> {
                    // Delete old document if ID changed
                    if (!oldId.equals(newId)) {
                        db.collection("tables").document(oldId)
                                .delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Table updated to Table " + newTableNumber, Toast.LENGTH_SHORT).show();
                                    loadExistingTables();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error deleting old table", e);
                                    Toast.makeText(this, "Warning: Old table not deleted", Toast.LENGTH_SHORT).show();
                                    loadExistingTables();
                                });
                    } else {
                        Toast.makeText(this, "Table updated successfully", Toast.LENGTH_SHORT).show();
                        loadExistingTables();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating table", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Show confirmation dialog before deleting
     */
    private void showDeleteConfirmation(TableData table) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Table")
                .setMessage("Are you sure you want to delete Table " + table.tableNumber + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTable(table))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Delete table from Firestore
     */
    private void deleteTable(TableData table) {
        db.collection("tables").document(table.id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Table " + table.tableNumber + " deleted", Toast.LENGTH_SHORT).show();
                    loadExistingTables();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting table", e);
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Generate QR code bitmap
     */
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

    /**
     * Table data model with tableNumber field
     */
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

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_tables) {
            // Already here
        } else if (id == R.id.nav_preview) {
            startActivity(new Intent(this, MenuPreviewActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();
        // Reload tables when returning to this activity
        loadExistingTables();
    }
}