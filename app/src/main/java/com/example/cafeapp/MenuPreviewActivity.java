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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuPreviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewPreview;
    private Spinner spinnerCategory;
    private MenuItemAdapter adapter;
    private ArrayList<MenuItem> menuItems;
    private List<String> categories;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_preview);

        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listViewPreview = findViewById(R.id.listViewPreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItems, true); // Customer view
        listViewPreview.setAdapter(adapter);

        loadCategoriesFromFirestore();
    }

    // ... (All other methods like loadCategoriesFromFirestore and loadMenuItems remain the same)

    private void loadCategoriesFromFirestore() {
        db.collection("menuItems").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Set<String> categorySet = new HashSet<>();
            categorySet.add("All");
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String category = doc.getString("category");
                if (category != null && !category.trim().isEmpty()) {
                    categorySet.add(category.trim());
                }
            }
            categories = new ArrayList<>(categorySet);
            categories.sort((a, b) -> a.equals("All") ? -1 : b.equals("All") ? 1 : a.compareToIgnoreCase(b));

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(spinnerAdapter);

            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    loadMenuItems(parent.getItemAtPosition(position).toString());
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            loadMenuItems("All");
        });
    }

    private void loadMenuItems(String categoryFilter) {
        Query query = "All".equals(categoryFilter) ? db.collection("menuItems") : db.collection("menuItems").whereEqualTo("category", categoryFilter);
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            menuItems.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                menuItems.add(new MenuItem(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("description"),
                        doc.getDouble("price"),
                        doc.getString("category"),
                        doc.getString("imageBase64")
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
        } else if (id == R.id.nav_barista) {
            startActivity(new Intent(this, BaristaActivity.class));
        } else if (id == R.id.nav_manage_staff) {
            startActivity(new Intent(this, ManageStaffActivity.class)); // <-- FIXED
        } else if (id == R.id.nav_logout) {
            mAuth.signOut(); // <-- FIXED
            Intent intent = new Intent(this, Loginactivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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