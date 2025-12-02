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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewMenu;
    private Button btnAddItem;
    private ArrayList<MenuItem> menuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listViewMenu = findViewById(R.id.listViewMenu);
        btnAddItem = findViewById(R.id.btnAddItem);
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItems, false);
        listViewMenu.setAdapter(adapter);

        btnAddItem.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, addmenuitem.class)));
        loadMenuItems();
    }

    private void loadMenuItems() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            if (error != null) { return; }
            if (value == null) { return; }
            menuItems.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
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
            // Already here
        } else if (id == R.id.nav_tables) {
            startActivity(new Intent(this, SetupTablesActivity.class));
        } else if (id == R.id.nav_preview) {
            startActivity(new Intent(this, MenuPreviewActivity.class));
        } else if (id == R.id.nav_barista) {
            startActivity(new Intent(this, BaristaActivity.class));
        } else if (id == R.id.nav_manage_staff) {
            startActivity(new Intent(this, ManageStaffActivity.class));
        } else if (id == R.id.nav_admin_profile) { // <-- ADDED
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
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