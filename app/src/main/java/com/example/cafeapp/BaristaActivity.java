package com.example.cafeapp;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * BaristaActivity - shows incoming (non-finished) orders.
 * Inherits drawer/toolbar/navigation behaviour from LogicAct.
 */
public class BaristaActivity extends LogicAct {

    private ListView listViewOrders;
    private ArrayList<Order> orderList;
    private OrderAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT: layout must contain views with these IDs:
        // - DrawerLayout: @+id/drawer_layout
        // - NavigationView: @+id/nav_view
        // - Toolbar: @+id/toolbar
        // If your layout used different IDs (e.g. toolbar_barista or nav_view_barista)
        // change them to the three IDs above or update LogicAct accordingly.
        setContentView(R.layout.activity_barista);

        // Let base class wire toolbar + drawer + nav + fetch role
        setupDrawer();
        toolbar = findViewById(R.id.toolbar);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Firebase auth & firestore are initialized in LogicAct.onCreate()
        // (mAuth and db are protected members in LogicAct)

        // Setup UI references
        listViewOrders = findViewById(R.id.listViewOrders);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(this, orderList);
        listViewOrders.setAdapter(adapter);

        // Setup refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::refreshOrders);

        // Start listening for orders
        listenForOrders();
    }

    private void listenForOrders() {
        if (db == null) return;
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
        if (db == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

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
    public void onBackPressed() {
        // drawerLayout is inherited from LogicAct and initialized by setupDrawer()
        if (drawerLayout != null && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
