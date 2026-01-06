package com.example.cafeapp;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageStaffActivity extends LogicAct {

    private ListView listViewStaff;
    private Button btnAddStaff;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StaffAdapter adapter;
    private List<Staff> staffList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_staff);
        setupDrawer(); // this sets up toolbar, toggle, nav listener

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        listViewStaff = findViewById(R.id.listViewStaff);
        btnAddStaff = findViewById(R.id.btnAddStaff);

        staffList = new ArrayList<>();
        adapter = new StaffAdapter(this, staffList);
        listViewStaff.setAdapter(adapter);

        btnAddStaff.setOnClickListener(v -> showAddStaffDialog());

        loadStaff();
    }

    private void loadStaff() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading staff", Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                staffList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Staff staff = doc.toObject(Staff.class);
                    // Ensure you don't add the admin to this list to prevent self-deactivation
                    if (!"Admin".equals(staff.getRole())) {
                        staffList.add(staff);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showAddStaffDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Staff Member");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Email");
        layout.addView(emailInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        final Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Barista", "Support"});
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user in Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            // Now save user role and status in Firestore
                            Map<String, Object> user = new HashMap<>();
                            user.put("uid", uid);
                            user.put("email", email);
                            user.put("role", role);
                            user.put("active", true); // Default to active

                            db.collection("users").document(uid).set(user)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(ManageStaffActivity.this, "Staff added successfully.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(ManageStaffActivity.this, "Failed to save staff details.", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(ManageStaffActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
