package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class Loginactivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginactivity);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
                return;
            }
            loginUser(email, password);
        });
    }
    // In your LoginActivity, after a successful login
    private void checkUserRoleAndRedirect(FirebaseUser user) {
        DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("role");
                if ("Admin".equals(role)) {
                    startActivity(new Intent(this, MenuForAdminActivity.class));
                } else if ("Barista".equals(role)) {
                    startActivity(new Intent(this, BaristaActivity.class));
                } else {
                    // Redirect to a default customer view or menu preview
                    startActivity(new Intent(this, MenuForClientActivity.class));
                }
                finish();
            } else {
                // Handle case where user document doesn't exist
                Toast.makeText(this, "User role not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        }
                    } else {
                        Toast.makeText(Loginactivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        boolean isActive = documentSnapshot.getBoolean("active");

                        if (!isActive) {
                            Toast.makeText(this, "Your account is deactivated.", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Sign out the deactivated user
                            return;
                        }

                        if ("Admin".equals(role)) {
                            // User is an Admin, go to MainActivity
                            Intent intent = new Intent(Loginactivity.this, MenuForAdminActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if ("Barista".equals(role)) {
                            // User is a Barista, go to BaristaActivity
                            Intent intent = new Intent(Loginactivity.this, BaristaActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Role not recognized.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // This can happen if the user exists in Auth but not in Firestore.
                        Toast.makeText(this, "User data not found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Loginactivity.this, "Failed to retrieve user role.", Toast.LENGTH_SHORT).show();
                });
    }
}