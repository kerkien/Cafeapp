package com.example.cafeapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

// ✅ Import your custom MenuItem class
import com.example.cafeapp.MenuItem;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewMenu;
    private Button btnAddItem;
    private ArrayList<MenuItem> menuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Initialize Firebase FIRST
        FirebaseApp.initializeApp(this);
        // Disable offline persistence to avoid cache issues
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        setContentView(R.layout.activity_main);

        // ✅ Toolbar and Drawer setup
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

        // ✅ Initialize UI elements
        listViewMenu = findViewById(R.id.listViewMenu);
        btnAddItem = findViewById(R.id.btnAddItem);

        // ✅ Initialize Firestore and adapter
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItems);
        listViewMenu.setAdapter(adapter);

        // ✅ Add new menu item
        btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, addmenuitem.class))
        );

        // ✅ Load menu items from Firestore
        loadMenuItems();
    }

    /**
     * Load all menu items from Firestore.
     */
    private void loadMenuItems() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Error fetching menu items", error);
                return;
            }

            if (value == null) {
                Log.e("Firestore", "No data returned");
                return;
            }

            menuItems.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Double price = doc.getDouble("price");
                String imageBase64 = doc.getString("imageBase64");

                Log.d("Firestore", "Loaded item: " + name + " (" + category + ")");

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
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_menu) {
            // Already on this screen
        } else if (id == R.id.nav_tables) {
            startActivity(new Intent(this, SetupTablesActivity.class));
        } else if (id == R.id.nav_preview) {
            startActivity(new Intent(this, MenuPreviewActivity.class));
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
