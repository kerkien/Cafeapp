package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Loginactivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnClient, btnAdmin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginactivity);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnClient = findViewById(R.id.btnClient);
        btnAdmin = findViewById(R.id.btnAdmin);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginAdmin());

        btnClient.setOnClickListener(v -> {
            startActivity(new Intent(Loginactivity.this, Landingactivity.class));
            finish(); // ✅ Removes Login from back stack
        });

        btnAdmin.setOnClickListener(v -> {
            // Currently launches same activity - change this later
            startActivity(new Intent(this, Loginactivity.class));
        });
    }

    private void loginAdmin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Loginactivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Loginactivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(Loginactivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}