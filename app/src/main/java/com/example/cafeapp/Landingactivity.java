package com.example.cafeapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

import java.util.List;

public class Landingactivity extends AppCompatActivity {

    private DecoratedBarcodeView scannerView;
    private Button btnClient, btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landingactivity);

        scannerView = findViewById(R.id.scannerView);
        btnClient = findViewById(R.id.btnClient);
        btnAdmin = findViewById(R.id.btnAdmin);

        // Start camera for scanning immediately
        scannerView.decodeContinuous(new BarcodeCallback() {
            private boolean scanned = false; // ✅ Flag to prevent multiple scans

            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!scanned && result.getText() != null && !result.getText().isEmpty()) {
                    scanned = true; // stop further scans
                    String scannedText = result.getText();
                    Intent intent = new Intent(Landingactivity.this, MenuForTableActivity.class);
                    intent.putExtra("tableId", extractTableId(scannedText));
                    startActivity(intent);
                }
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) { }
        });


        btnClient.setOnClickListener(v -> {
            Toast.makeText(this, "Scan QR code on table to view menu", Toast.LENGTH_SHORT).show();
        });

        btnAdmin.setOnClickListener(v -> startActivity(new Intent(this, Loginactivity.class)));
    }

    private String extractTableId(String url) {
        // Example: https://yourapp.com/menu?table=1
        Uri uri = Uri.parse(url);
        return "Table_" + uri.getQueryParameter("table");
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.pause();
    }
}
