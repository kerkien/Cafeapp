package com.example.cafeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class Landingactivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private DecoratedBarcodeView scannerView;
    private Button btnClient, btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ CRITICAL: Disable persistence BEFORE any other Firebase operations
        FirebaseApp.initializeApp(this);
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        setContentView(R.layout.activity_landingactivity);

        scannerView = findViewById(R.id.scannerView);
        btnClient = findViewById(R.id.btnClient);
        btnAdmin = findViewById(R.id.btnAdmin);

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        } else {
            startScanning();
        }

        btnClient.setOnClickListener(v ->
                Toast.makeText(this, "Scan QR code on table to view menu", Toast.LENGTH_SHORT).show()
        );

        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, Loginactivity.class))
        );
    }

    private void startScanning() {
        scannerView.decodeContinuous(new BarcodeCallback() {
            private boolean scanned = false; // Prevent multiple scans

            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!scanned && result.getText() != null && !result.getText().isEmpty()) {
                    scanned = true;
                    String scannedText = result.getText();

                    // Handle QR result and navigate
                    Intent intent = new Intent(Landingactivity.this, MenuForTableActivity.class);
                    intent.setData(Uri.parse(scannedText)); // Pass the full URL
                    startActivity(intent);

                    // Optional: Stop scanning after success
                    scannerView.pause();
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Not used
            }
        });
        scannerView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scannerView != null) {
            scannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scannerView != null) {
            scannerView.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }
}
