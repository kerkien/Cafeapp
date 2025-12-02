package com.example.cafeapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderTrackingActivity extends AppCompatActivity {

    private TextView txtOrderStatus;
    private ProgressBar progressBar;
    private Button btnDone;
    private FirebaseFirestore db;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        txtOrderStatus = findViewById(R.id.txtOrderStatusClient);
        progressBar = findViewById(R.id.progressBar);
        btnDone = findViewById(R.id.btnDoneClient);
        db = FirebaseFirestore.getInstance();

        orderId = getIntent().getStringExtra("orderId");

        if (orderId != null) {
            listenForOrderStatus();
        }

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(OrderTrackingActivity.this, Landingactivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void listenForOrderStatus() {
        db.collection("orders").document(orderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        txtOrderStatus.setText("Error");
                        return;
                    }

                    String status = snapshot.getString("status");
                    if (status != null) {
                        txtOrderStatus.setText(status);
                        switch (status) {
                            case "Preparing":
                                txtOrderStatus.setTextColor(Color.BLUE);
                                progressBar.setProgress(1);
                                break;
                            case "Finished":
                                txtOrderStatus.setTextColor(Color.GREEN);
                                progressBar.setProgress(2);
                                break;
                            case "Pending":
                            default:
                                txtOrderStatus.setTextColor(Color.parseColor("#FFA500")); // Orange
                                progressBar.setProgress(0);
                                break;
                        }
                    }
                });
    }
}