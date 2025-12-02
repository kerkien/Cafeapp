package com.example.cafeapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtProfileEmail, txtProfileRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        txtProfileRole = findViewById(R.id.txtProfileRole);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String uid = currentUser.getUid();
            txtProfileEmail.setText(email != null ? email : "No Email");

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            txtProfileRole.setText(role != null ? role : "No Role Assigned");
                        } else {
                            txtProfileRole.setText("Role not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load user role.", Toast.LENGTH_SHORT).show();
                        txtProfileRole.setText("Error");
                    });
        } else {
            txtProfileEmail.setText("Not logged in");
            txtProfileRole.setText("Unknown");
        }
    }
}