package com.example.cafeapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;

public class BaristaActivity extends LogicAct {

    private static final String TAG = "BaristaActivity";
    private static final long AUTO_REFRESH_INTERVAL = 5000; // 5 seconds

    private ListView listViewOrders;
    private ArrayList<Order> orderList;
    private OrderAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner spinnerTimeFilter;
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barista);
        setupDrawer();

        listViewOrders = findViewById(R.id.listViewOrders);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(this, orderList);
        listViewOrders.setAdapter(adapter);

        // Setup time filter
        setupTimeFilter();

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshOrders);

        // Setup auto-refresh
        setupAutoRefresh();

        // Initial load
        listenForOrders();
    }

    private void setupTimeFilter() {
        String[] timeFilters = {"All Time", "Today", "Last Hour", "Last 30 mins"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, timeFilters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeFilter.setAdapter(filterAdapter);

        spinnerTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listenForOrders();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Auto-refreshing orders...");
                refreshOrders();
                refreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Starting auto-refresh");
        refreshOrders();
        refreshHandler.postDelayed(refreshRunnable, AUTO_REFRESH_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - Stopping auto-refresh");
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private long getTimeFilterMillis() {
        int position = spinnerTimeFilter.getSelectedItemPosition();
        Calendar calendar = Calendar.getInstance();

        switch (position) {
            case 1: // Today
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTimeInMillis();
            case 2: // Last Hour
                return System.currentTimeMillis() - (60 * 60 * 1000);
            case 3: // Last 30 mins
                return System.currentTimeMillis() - (30 * 60 * 1000);
            default: // All Time
                return 0;
        }
    }

    private void listenForOrders() {
        if (db == null) return;

        long timeFilter = getTimeFilterMillis();
        Query query = db.collection("orders")
                .whereNotEqualTo("status", "Ready")
                .orderBy("status")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (timeFilter > 0) {
            query = query.whereGreaterThan("timestamp", new java.util.Date(timeFilter));
        }

        query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed", e);
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
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        long timeFilter = getTimeFilterMillis();
        Query query = db.collection("orders")
                .whereNotEqualTo("status", "Ready")
                .orderBy("status")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (timeFilter > 0) {
            query = query.whereGreaterThan("timestamp", new java.util.Date(timeFilter));
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateOrderList(queryDocumentSnapshots.getDocuments());
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    Log.d(TAG, "Orders refreshed: " + orderList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Refresh failed", e);
                    Toast.makeText(BaristaActivity.this, "Failed to refresh orders.", Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void updateOrderList(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        orderList.clear();
        for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
            Order order = doc.toObject(Order.class);
            if (order != null) {
                order.setId(doc.getId());
                orderList.add(order);
                Log.d(TAG, "Order " + doc.getId() + ": " + order.getStatus() +
                        ", items: " + (order.getItems() != null ? order.getItems().size() : 0));
            }
        }
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Order list updated: " + orderList.size() + " orders");
    }
}