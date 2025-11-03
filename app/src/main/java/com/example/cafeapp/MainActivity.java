package com.example.cafeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ListView listViewMenu;
    private Button btnAddItem;
    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewMenu = findViewById(R.id.listViewMenu);
        btnAddItem = findViewById(R.id.btnAddItem);

        db = FirebaseFirestore.getInstance();
        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listViewMenu.setAdapter(adapter);

        btnAddItem.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, addmenuitem.class);
            startActivity(intent);
        });

        loadMenuItems();
    }

    private void loadMenuItems() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            itemList.clear();
            if (value != null) {
                for (DocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    Double price = doc.getDouble("price");
                    String category = doc.getString("category");
                    itemList.add(name + " - $" + price + " (" + category + ")");
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
