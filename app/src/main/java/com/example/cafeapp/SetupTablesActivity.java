package com.example.cafeapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class SetupTablesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText edtTableCount;
    private Button btnGenerate;
    private LinearLayout layoutQrContainer;
    private FirebaseFirestore db;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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

        // Original setup logic
        edtTableCount = findViewById(R.id.edtTableCount);
        btnGenerate = findViewById(R.id.btnGenerate);
        layoutQrContainer = findViewById(R.id.layoutQrContainer);

        db = FirebaseFirestore.getInstance();
        btnGenerate.setOnClickListener(v -> generateTableQRCodes());
    }

    private void generateTableQRCodes() {
        String input = edtTableCount.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter number of tables", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = Integer.parseInt(input);
        layoutQrContainer.removeAllViews();

        for (int i = 1; i <= count; i++) {
            String tableId = "Table_" + i;
            String qrContent = "https://yourapp.com/menu?table=" + i;

            Bitmap qrBitmap = generateQRCode(qrContent);
            if (qrBitmap != null) {
                TextView txtTable = new TextView(this);
                txtTable.setText("Table " + i);
                txtTable.setTextSize(16);
                txtTable.setPadding(0, 16, 0, 8);
                layoutQrContainer.addView(txtTable);

                ImageView qrImage = new ImageView(this);
                qrImage.setImageBitmap(qrBitmap);
                qrImage.setPadding(0, 0, 0, 16);
                layoutQrContainer.addView(qrImage);

                int finalI = i;
                db.collection("tables").document(tableId)
                        .set(new Table(tableId, qrContent))
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Table " + finalI + " saved", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error saving Table " + finalI + ": " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
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

    public static class Table {
        public String id;
        public String qrLink;

        public Table() {}
        public Table(String id, String qrLink) {
            this.id = id;
            this.qrLink = qrLink;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            // Navigate to Create Menu screen (MainActivity)
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_tables) {
            // Already here (SetupTablesActivity)
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
}