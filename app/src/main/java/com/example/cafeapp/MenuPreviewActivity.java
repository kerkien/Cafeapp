package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MenuPreviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewPreview;
    private Spinner spinnerCategory;
    private MenuItemAdapter adapter;
    private ArrayList<com.example.cafeapp.MenuItem> menuItems;
    private ArrayList<String> categories;
    private FirebaseFirestore db;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_preview);



        // Toolbar and drawer setup
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

        // Initialize UI
        listViewPreview = findViewById(R.id.listViewPreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItems);
        listViewPreview.setAdapter(adapter);

        // ✅ Load categories dynamically
        loadCategoriesFromFirestore();

        // Load all menu items initially
        loadMenuItems("All");
    }

    /**
     * Load unique categories from Firestore dynamically.
     */
    private void loadCategoriesFromFirestore() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            Set<String> categorySet = new HashSet<>();
            categorySet.add("All"); // Always include "All"

            for (DocumentSnapshot doc : value.getDocuments()) {
                String category = doc.getString("category");
                if (category != null && !category.trim().isEmpty()) {
                    categorySet.add(category.trim());
                }
            }

            // Convert to list for the spinner
            categories = new ArrayList<>(categorySet);

            // Sort alphabetically except "All" on top
            categories.sort((a, b) -> {
                if (a.equals("All")) return -1;
                if (b.equals("All")) return 1;
                return a.compareToIgnoreCase(b);
            });

            // Update Spinner UI
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    categories
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(spinnerAdapter);

            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCategory = parent.getItemAtPosition(position).toString();
                    loadMenuItems(selectedCategory);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        });
    }

    /**
     * Load menu items based on selected category.
     */
    private void loadMenuItems(String categoryFilter) {
        Query query = db.collection("menuItems").limit(50);

        if (!categoryFilter.equals("All")) {
            query = query.whereEqualTo("category", categoryFilter).limit(50);
        }


        query.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            menuItems.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Double price = doc.getDouble("price");
                String imageBase64 = doc.getString("imageBase64");

                menuItems.add(new com.example.cafeapp.MenuItem(
                        id,
                        name != null ? name : "",
                        description != null ? description : "",
                        price != null ? price : 0.0,
                        category != null ? category : "",
                        imageBase64 != null ? imageBase64 : ""
                ));
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_tables) {
            startActivity(new Intent(this, SetupTablesActivity.class));
        } else if (id == R.id.nav_preview) {
            // Already here
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
