package com.example.cafeapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends LogicAct {

    private static final String TAG = "Dashboard";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private BarChart barChart;
    private FirebaseFirestore db;
    private TextView txtTotalSales;
    private TextView txtTotalOrders;
    private ListView listMostSold;
    private Button btnExportCSV;

    private List<SalesItem> salesItemsList;
    private SalesItemAdapter salesAdapter;
    private double totalSales = 0.0;
    private int totalOrders = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupDrawer();

        barChart = findViewById(R.id.barChart);
        txtTotalSales = findViewById(R.id.txtTotalSales);
        txtTotalOrders = findViewById(R.id.txtTotalOrders);
        listMostSold = findViewById(R.id.listMostSold);
        btnExportCSV = findViewById(R.id.btnExportCSV);

        db = FirebaseFirestore.getInstance();
        salesItemsList = new ArrayList<>();
        salesAdapter = new SalesItemAdapter(this, salesItemsList);
        listMostSold.setAdapter(salesAdapter);

        btnExportCSV.setOnClickListener(v -> exportToCSV());

        loadSalesData();
    }

    private void loadSalesData() {
        Log.d(TAG, "Loading sales data...");

        // Get ALL orders (don't filter by status initially)
        db.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, SalesItem> salesByItem = new HashMap<>();
                    totalSales = 0.0;
                    totalOrders = 0;

                    Log.d(TAG, "Total documents in orders collection: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        Log.d(TAG, "Order ID: " + document.getId() + ", Status: " + status);

                        // Count all orders regardless of status, or filter if needed
                        // Remove status filter to see ALL orders:
                        // if (status != null && (status.equals("Ready") || status.equals("Finished"))) {

                        totalOrders++;

                        List<Map<String, Object>> items = (List<Map<String, Object>>) document.get("items");

                        if (items != null) {
                            Log.d(TAG, "  Items in order: " + items.size());

                            for (Map<String, Object> item : items) {
                                String itemName = (String) item.get("name");

                                Double itemPrice = null;
                                Object priceObj = item.get("price");
                                if (priceObj instanceof Double) {
                                    itemPrice = (Double) priceObj;
                                } else if (priceObj instanceof Long) {
                                    itemPrice = ((Long) priceObj).doubleValue();
                                } else if (priceObj instanceof Integer) {
                                    itemPrice = ((Integer) priceObj).doubleValue();
                                }

                                Object quantityObj = item.get("quantity");
                                int quantity = 1;
                                if (quantityObj instanceof Long) {
                                    quantity = ((Long) quantityObj).intValue();
                                } else if (quantityObj instanceof Integer) {
                                    quantity = (Integer) quantityObj;
                                }

                                if (itemName != null && itemPrice != null) {
                                    double revenue = itemPrice * quantity;
                                    totalSales += revenue;

                                    Log.d(TAG, "    Item: " + itemName + ", Qty: " + quantity + ", Price: $" + itemPrice + ", Revenue: $" + revenue);

                                    if (salesByItem.containsKey(itemName)) {
                                        SalesItem existingItem = salesByItem.get(itemName);
                                        existingItem.quantity += quantity;
                                        existingItem.revenue += revenue;
                                    } else {
                                        salesByItem.put(itemName, new SalesItem(itemName, quantity, revenue));
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "  No items found in order");
                        }
                        // }
                    }

                    Log.d(TAG, "Final totals - Sales: $" + totalSales + ", Orders: " + totalOrders);

                    // Update summary cards
                    txtTotalSales.setText(String.format("$%.2f", totalSales));
                    txtTotalOrders.setText(String.valueOf(totalOrders));

                    // Convert to list and sort by quantity (most sold first)
                    salesItemsList.clear();
                    salesItemsList.addAll(salesByItem.values());
                    Collections.sort(salesItemsList, (a, b) -> Integer.compare(b.quantity, a.quantity));

                    Log.d(TAG, "Sales items list size: " + salesItemsList.size());

                    salesAdapter.notifyDataSetChanged();
                    setupChart(salesByItem);

                    if (totalOrders == 0) {
                        msg("No orders found in database");
                    } else {
                        msg("Loaded " + totalOrders + " orders successfully");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load sales data", e);
                    msg("Failed to load sales data: " + e.getMessage());
                });
    }

    private void setupChart(Map<String, SalesItem> salesByItem) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (salesByItem.isEmpty()) {
            Log.d(TAG, "No sales data to display in chart");
            barChart.clear();
            barChart.setNoDataText("No sales data available");
            return;
        }

        // Sort items by revenue for chart
        List<SalesItem> sortedItems = new ArrayList<>(salesByItem.values());
        Collections.sort(sortedItems, (a, b) -> Double.compare(b.revenue, a.revenue));

        // Take top 10 items for chart
        int maxItems = Math.min(10, sortedItems.size());
        for (int i = 0; i < maxItems; i++) {
            SalesItem item = sortedItems.get(i);
            entries.add(new BarEntry(i, (float) item.revenue));
            labels.add(item.itemName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Revenue by Item");
        dataSet.setColor(0xFFC67C4E);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void exportToCSV() {
        if (salesItemsList.isEmpty()) {
            msg("No data to export");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        performCSVExport();
    }

    private void performCSVExport() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "sales_report_" + timestamp + ".csv";

            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    msg("Failed to create file");
                    return;
                }
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(downloadDir, fileName);
                outputStream = new java.io.FileOutputStream(file);
            }

            if (outputStream != null) {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);

                // Write CSV header
                writer.write("Item Name,Quantity Sold,Revenue\n");

                // Write data rows
                for (SalesItem item : salesItemsList) {
                    writer.write(String.format("%s,%d,$%.2f\n",
                            item.itemName, item.quantity, item.revenue));
                }

                // Write summary
                writer.write(String.format("\nTotal Orders,%d,\n", totalOrders));
                writer.write(String.format("Total Sales,,$%.2f\n", totalSales));

                writer.close();
                outputStream.close();

                msg("Report exported successfully to Downloads");
                Log.d(TAG, "CSV exported: " + fileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to export CSV", e);
            msg("Failed to export report: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performCSVExport();
            } else {
                msg("Permission denied. Cannot export CSV.");
            }
        }
    }

    // SalesItem model
    private static class SalesItem {
        String itemName;
        int quantity;
        double revenue;

        SalesItem(String itemName, int quantity, double revenue) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }

    // Adapter for sales items list
    private class SalesItemAdapter extends ArrayAdapter<SalesItem> {

        SalesItemAdapter(DashboardActivity context, List<SalesItem> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_sales_row, parent, false);
            }

            SalesItem item = getItem(position);
            if (item != null) {
                TextView txtItemName = convertView.findViewById(R.id.txtItemName);
                TextView txtQuantitySold = convertView.findViewById(R.id.txtQuantitySold);
                TextView txtRevenue = convertView.findViewById(R.id.txtRevenue);

                txtItemName.setText(item.itemName);
                txtQuantitySold.setText(String.valueOf(item.quantity)); // This should show quantity
                txtRevenue.setText(String.format("$%.2f", item.revenue));

                Log.d(TAG, "Displaying: " + item.itemName + ", Qty: " + item.quantity);
            }

            return convertView;
        }
    }
}