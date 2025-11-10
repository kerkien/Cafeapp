package com.example.cafeapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class SetupTablesActivity extends AppCompatActivity {

    private EditText edtTableCount;
    private Button btnGenerate;
    private LinearLayout layoutQrContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_tables);

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
        layoutQrContainer.removeAllViews(); // Clear previous QR codes

        for (int i = 1; i <= count; i++) {
            String tableId = "Table_" + i;
            String qrContent = "https://yourapp.com/menu?table=" + i;

            Bitmap qrBitmap = generateQRCode(qrContent);
            if (qrBitmap != null) {
                // Display table label
                TextView txtTable = new TextView(this);
                txtTable.setText("Table " + i);
                txtTable.setTextSize(16);
                txtTable.setPadding(0, 16, 0, 8);
                layoutQrContainer.addView(txtTable);

                // Display QR code
                ImageView qrImage = new ImageView(this);
                qrImage.setImageBitmap(qrBitmap);
                qrImage.setPadding(0, 0, 0, 16);
                layoutQrContainer.addView(qrImage);

                // Save table info to Firestore
                int finalI = i; // For Toast inside listener
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

    // 🔹 Table model for Firestore
    public static class Table {
        public String id;
        public String qrLink;

        public Table() {} // Required by Firestore

        public Table(String id, String qrLink) {
            this.id = id;
            this.qrLink = qrLink;
        }
    }
}
