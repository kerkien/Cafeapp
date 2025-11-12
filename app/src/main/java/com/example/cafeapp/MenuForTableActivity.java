package com.example.cafeapp;

import android.content.Intent;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MenuForTableActivity extends AppCompatActivity {

    private ListView listViewMenu;
    private ArrayList<MenuItem> menuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;
    private Button btnScanAgain;
    private TextView txtTableNumber;

    private String tableId; // Get from scanned QR

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_for_table);

        // Initialize views
        listViewMenu = findViewById(R.id.listViewMenu);
        btnScanAgain = findViewById(R.id.btnScanAgain);
        txtTableNumber = findViewById(R.id.txtTableNumber);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();

        // ✅ Use 'true' for preview mode (hide edit/delete icons)
        adapter = new MenuItemAdapter(this, menuItems, true);
        listViewMenu.setAdapter(adapter);

        // ✅ Get tableId from Intent
        tableId = getIntent().getStringExtra("tableId");
        if (tableId == null) tableId = "Table_1"; // Default value

        // ✅ Display table number properly
        txtTableNumber.setText("Table: " + tableId.replace("Table_", ""));

        // ✅ Load menu items from Firestore
        loadMenuItems();

        // ✅ Button to rescan QR code
        btnScanAgain.setOnClickListener(v -> {
            Intent intent = new Intent(MenuForTableActivity.this, Landingactivity.class);
            startActivity(intent);
            finish(); // Close current activity
        });
    }

    private void loadMenuItems() {
        db.collection("menuItems").get().addOnSuccessListener(querySnapshot -> {
            menuItems.clear();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Double price = doc.getDouble("price");
                String imageBase64 = doc.getString("imageBase64");

                menuItems.add(new MenuItem(
                        id,
                        name != null ? name : "",
                        description != null ? description : "",
                        price != null ? price : 0.0,
                        category != null ? category : "",
                        imageBase64 != null ? imageBase64 : ""
                ));
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show()
        );
    }
}
