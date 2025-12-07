package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public abstract class LogicAct extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Call this AFTER setContentView() in every child activity
     */
    protected void setupDrawer() {

        // TOOLBAR -----------------------------------
        toolbar = findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // DRAWER ------------------------------------
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (drawerLayout != null && toolbar != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );

            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        // Prevent UI from sliding under status bar
        if (drawerLayout != null)
            drawerLayout.setFitsSystemWindows(true);

        if (navigationView != null)
            navigationView.setFitsSystemWindows(true);

//        // If there is a gear button in layout, connect it to drawer
//        ImageView gear = findViewById(R.id.btnGear);
//        if (gear != null) {
//            gear.setOnClickListener(v -> openDrawer());
//        }
    }

    // NAVIGATION MENU HANDLER -----------------------------------------------------

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_tables) {
            go(SetupTablesActivity.class);

        } else if (id == R.id.nav_preview) {
            go(MenuForAdminActivity.class);


        }else if (id == R.id.nav_manage_staff) {
            go(ManageStaffActivity.class);
        } else if (id == R.id.nav_dashboard) {
            go(DashboardActivity.class);
        } else if (id == R.id.nav_admin_profile) {
            go(ProfileActivity.class);

        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(this, Loginactivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        closeDrawer();
        return true;
    }

    // DRAWER SHORTCUTS -----------------------------------------------------------

    protected void openDrawer() {
        if (drawerLayout != null)
            drawerLayout.openDrawer(GravityCompat.START);
    }

    protected void closeDrawer() {
        if (drawerLayout != null)
            drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    // UTIL METHODS ----------------------------------------------------------------

    protected void go(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    protected void msg(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
