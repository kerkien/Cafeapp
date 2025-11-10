package com.example.cafeapp;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class MenuPreviewActivity extends AppCompatActivity {

    private ListView listViewPreview;
    private MenuItemAdapter adapter;
    private ArrayList<MenuItem> menuItems;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_preview);

        listViewPreview = findViewById(R.id.listViewPreview);
        db = FirebaseFirestore.getInstance();
        menuItems = new ArrayList<>();

        adapter = new MenuItemAdapter(this, menuItems);
        listViewPreview.setAdapter(adapter);

        loadMenuItems();
    }

    private void loadMenuItems() {
        db.collection("menuItems").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            menuItems.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Double price = doc.getDouble("price");
                String imageBase64 = doc.getString("imageBase64");

                menuItems.add(new MenuItem(
                        id,
                        name != null ? name : "",
                        description != null ? description : "",
                        price != null ? price : 0.0,
                        category != null ? category : "",
                        imageBase64 != null ? imageBase64 : ""
                ));
            }
            adapter.notifyDataSetChanged();
        });
    }
}
