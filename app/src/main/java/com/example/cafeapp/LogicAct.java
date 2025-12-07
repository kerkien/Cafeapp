package com.example.cafeapp;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Base Activity that wires Toolbar + Drawer + Navigation and provides role-aware menu handling.
 */
public abstract class LogicAct extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "LogicAct";

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    protected FirebaseAuth mAuth;
    protected FirebaseFirestore db;
    protected ActionBarDrawerToggle toggle;

    // current role value loaded from firestore (e.g. "admin", "barista")
    protected String currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Call this AFTER setContentView() in every child activity so views can be found.
     */
    protected void setupDrawer() {

        // TOOLBAR -----------------------------------
        toolbar = findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        } else {
            Log.w(TAG, "Toolbar not found in layout. Make sure your layout has a Toolbar with id @id/toolbar");
        }

        // DRAWER ------------------------------------
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (drawerLayout != null && toolbar != null) {
            toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        } else {
            if (drawerLayout == null) Log.w(TAG, "DrawerLayout not found in layout (id: drawer_layout).");
        }

        if (drawerLayout != null) drawerLayout.setFitsSystemWindows(true);
        if (navigationView != null) navigationView.setFitsSystemWindows(true);

        // If we have a signed-in user, fetch their role and apply to the menu.
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            fetchAndApplyUserRole(user.getUid());
        } else {
            applyRoleToMenu(null);
        }
    }

    protected void fetchAndApplyUserRole(String uid) {
        if (db == null) {
            Log.w(TAG, "Firestore not initialized; cannot fetch role.");
            applyRoleToMenu(null);
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc != null && doc.exists()) {
                        String role = null;
                        try {
                            role = doc.getString("role");
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to read role field from user doc", e);
                        }
                        currentRole = role;
                        applyRoleToMenu(role);
                        Log.d(TAG, "Applied role to menu: " + role);
                    } else {
                        Log.w(TAG, "User document not found for uid: " + uid);
                        currentRole = null;
                        applyRoleToMenu(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user doc: " + e.getMessage(), e);
                    currentRole = null;
                    applyRoleToMenu(null);
                });
    }

    /**
     * Apply role to menu safely:
     * - first attempt to find the group id by name (so no compile-time dependency on R.id.group_admin)
     * - if not present, fall back to toggling specific items by id (nav_tables, nav_preview, nav_manage_staff)
     */
    protected void applyRoleToMenu(String role) {

        if (navigationView == null) return;
        Menu menu = navigationView.getMenu();

        boolean isAdmin = "admin".equalsIgnoreCase(role);
        boolean isBarista = "barista".equalsIgnoreCase(role);

        // Admin group (preview, tables, staff, dashboard)
        menu.setGroupVisible(R.id.group_admin, isAdmin);

        // Barista button
        MenuItem baristaItem = menu.findItem(R.id.nav_barista);
        if (baristaItem != null) baristaItem.setVisible(isBarista);

        // Profile and logout always visible
        menu.findItem(R.id.nav_profile).setVisible(true);
        menu.findItem(R.id.nav_logout).setVisible(true);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.nav_tables) {
            go(SetupTablesActivity.class);

        } else if (id == R.id.nav_preview) {
            go(MenuForAdminActivity.class);

        } else if (id == R.id.nav_manage_staff) {
            go(ManageStaffActivity.class);

        } else if (id == R.id.nav_dashboard) {
            go(DashboardActivity.class);

        } else if (id == R.id.nav_profile) {
            go(ProfileActivity.class);

        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(this, Loginactivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else if (id == R.id.nav_barista) {
            go(BaristaActivity.class);
        } else {
            Log.w(TAG, "Unhandled navigation item id: " + id);
        }

        closeDrawer();
        return true;
    }


    protected void openDrawer() {
        if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
    }

    protected void closeDrawer() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (toggle != null) toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (toggle != null) toggle.onConfigurationChanged(newConfig);
    }

    protected void go(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    protected void msg(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}