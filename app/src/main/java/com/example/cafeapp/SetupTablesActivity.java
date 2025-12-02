package com.example.cafeapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.OutputStream;
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
    private FirebaseAuth mAuth;

    private int currentMaxTableNumber = 0;
    private List<TableData> existingTables = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Bitmap bitmapToSave;
    private String fileNameToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_tables);

        mAuth = FirebaseAuth.getInstance();

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
                Toast.makeText(this, "QR Code not available", Toast.LENGTH_SHORT).show();
            }
        });

        layoutQrContainer.addView(cardView);
    }

    private void generateTableQRCodes() {
        String input = edtTableCount.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter number of tables", Toast.LENGTH_SHORT).show();
            return;
        }

        int count;
        try {
            count = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        int startNumber = currentMaxTableNumber + 1;

        for (int i = 0; i < count; i++) {
            int tableNumber = startNumber + i;
            String tableId = "Table_" + tableNumber;
            String qrContent = "https://yourapp.com/menu?table=" + tableNumber;

            TableData newTable = new TableData(tableId, tableNumber, qrContent);

            db.collection("tables").document(tableId).set(newTable);
        }
        edtTableCount.setText("");
        loadExistingTables();
    }

    private void showUpdateDialog(TableData table) {
        // Implementation from your previous code
    }

    private void showDeleteConfirmation(TableData table) {
        // Implementation from your previous code
    }

    private void handleDownloadRequest(Bitmap bitmap, String fileName) {
        // Implementation from your previous code
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

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_tables) {
            // Already here
        } else if (id == R.id.nav_preview) {
            startActivity(new Intent(this, MenuPreviewActivity.class));
        } else if (id == R.id.nav_barista) {
            startActivity(new Intent(this, BaristaActivity.class));
        } else if (id == R.id.nav_manage_staff) {
            startActivity(new Intent(this, ManageStaffActivity.class));
        } else if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
        } else if (id == R.id.nav_admin_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
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