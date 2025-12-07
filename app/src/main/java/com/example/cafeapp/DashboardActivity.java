package com.example.cafeapp;

import android.os.Bundle;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends LogicAct {

    private BarChart barChart;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupDrawer(); // this sets up toolbar, toggle, nav listener



        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        loadSalesData();
    }

    private void loadSalesData() {
        db.collection("orders")
                .whereEqualTo("status", "Finished")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Double> salesByItem = new HashMap<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        if (order.getItems() != null) {
                            for (Map<String, Object> item : order.getItems()) {
                                String itemName = (String) item.get("name");
                                Double itemPrice = (Double) item.get("price");
                                if (itemName != null && itemPrice != null) {
                                    salesByItem.put(itemName, salesByItem.getOrDefault(itemName, 0.0) + itemPrice);
                                }
                            }
                        }
                    }
                    setupChart(salesByItem);
                });
    }

    private void setupChart(Map<String, Double> salesByItem) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : salesByItem.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Sales by Item");
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        barChart.getDescription().setEnabled(false);
        barChart.invalidate(); // refresh
    }
}