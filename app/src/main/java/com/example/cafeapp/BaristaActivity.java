package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List; // <-- THE FIX IS HERE

public class BaristaActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listViewOrders;
    private ArrayList<Order> orderList;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    protected NavigationView navigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barista);
        navigationView = findViewById(R.id.nav_view);
        // Prevent UI from sliding under status bar
        if (drawerLayout != null)
            drawerLayout.setFitsSystemWindows(true);

        if (navigationView != null)
            navigationView.setFitsSystemWindows(true);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_barista);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_barista);
        NavigationView navigationView = findViewById(R.id.nav_view_barista);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listViewOrders = findViewById(R.id.listViewOrders);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        orderList = new ArrayList<>();
        adapter = new OrderAdapter(this, orderList);
        listViewOrders.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Setup refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::refreshOrders);

        listenForOrders();
    }

    private void listenForOrders() {
        db.collection("orders")
                .whereNotEqualTo("status", "Finished")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(BaristaActivity.this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        updateOrderList(snapshots.getDocuments());
                    }
                });
    }

    private void refreshOrders() {
        db.collection("orders")
                .whereNotEqualTo("status", "Finished")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateOrderList(queryDocumentSnapshots.getDocuments());
                    swipeRefreshLayout.setRefreshing(false); // Stop the refreshing indicator
                    Toast.makeText(BaristaActivity.this, "Orders refreshed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BaristaActivity.this, "Failed to refresh orders.", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void updateOrderList(List<DocumentSnapshot> documents) {
        orderList.clear();
        for (DocumentSnapshot doc : documents) {
            Order order = doc.toObject(Order.class);
            if (order != null) {
                order.setId(doc.getId());
                orderList.add(order);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_barista_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_barista_logout) {
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