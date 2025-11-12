package com.example.cafeapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewMenu;
    private Button btnAddItem;
    private ArrayList<MenuItem> menuItems;
    private MenuItemAdapter adapter;
    private FirebaseFirestore db;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private LinearLayout categoryContainer;
    private String selectedCategory = "All";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Toolbar + Drawer setup
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

        // ✅ UI components
        listViewMenu = findViewById(R.id.listViewMenu);
        btnAddItem = findViewById(R.id.btnAddItem);
        categoryContainer = findViewById(R.id.categoryContainer);

        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();

        // ✅ Show edit/delete since admin
        adapter = new MenuItemAdapter(this, menuItems, false);
        listViewMenu.setAdapter(adapter);

        btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, addmenuitem.class))
        );

        // ✅ Load categories + menu
        loadMenuItemsAndCategories();
    }

    private void loadMenuItemsAndCategories() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            menuItems.clear();
            Set<String> categories = new HashSet<>();
            categories.add("All"); // default

            for (DocumentSnapshot doc : value.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Double price = doc.getDouble("price");
                String imageBase64 = doc.getString("imageBase64");

                if (category != null && !category.isEmpty())
                    categories.add(category);

                menuItems.add(new MenuItem(
                        id,
                        name != null ? name : "",
                        description != null ? description : "",
                        price != null ? price : 0.0,
                        category != null ? category : "",
                        imageBase64 != null ? imageBase64 : ""
                ));
            }

            setupCategoryButtons(new ArrayList<>(categories));
            filterMenuByCategory(selectedCategory);
        });
    }

    private void setupCategoryButtons(ArrayList<String> categories) {
        categoryContainer.removeAllViews();

        for (String cat : categories) {
            TextView textView = new TextView(this);
            textView.setText(cat);
            textView.setTextSize(16);
            textView.setPadding(32, 16, 32, 16);
            textView.setTypeface(null, cat.equals(selectedCategory) ? Typeface.BOLD : Typeface.NORMAL);
            textView.setBackgroundResource(cat.equals(selectedCategory)
                    ? R.drawable.category_chip_selected
                    : R.drawable.category_chip_background);

            textView.setOnClickListener(v -> {
                selectedCategory = cat;
                setupCategoryButtons(categories);
                filterMenuByCategory(cat);
            });

            categoryContainer.addView(textView);
        }
    }

    private void filterMenuByCategory(String category) {
        ArrayList<MenuItem> filtered = new ArrayList<>();
        for (MenuItem item : menuItems) {
            if (category.equals("All") || item.getCategory().equalsIgnoreCase(category)) {
                filtered.add(item);
            }
        }
        adapter = new MenuItemAdapter(this, filtered, false);
        listViewMenu.setAdapter(adapter);
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
