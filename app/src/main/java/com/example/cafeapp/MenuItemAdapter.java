package com.example.cafeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class MenuItemAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<MenuItem> menuItems;
    private FirebaseFirestore db;
    private LayoutInflater inflater;

    public MenuItemAdapter(Context context, ArrayList<MenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems != null ? menuItems : new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return menuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return menuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.menu_item_layout, parent, false);
        }

        ImageView imgMenuItem = convertView.findViewById(R.id.imgMenuItem);
        TextView txtName = convertView.findViewById(R.id.txtName);
        TextView txtCategory = convertView.findViewById(R.id.txtCategory);
        TextView txtPrice = convertView.findViewById(R.id.txtPrice);
        ImageButton btnEdit = convertView.findViewById(R.id.btnEdit);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        MenuItem item = menuItems.get(position);

        // ✅ Set text
        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText("$" + item.getPrice());

        // ✅ Decode and show image
        String imageBase64 = item.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imgMenuItem.setImageBitmap(bitmap);
            } catch (Exception e) {
                imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // ✅ Delete button
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) ->
                        db.collection("menuItems").document(item.getId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                )
                )
                .setNegativeButton("No", null)
                .show());

        // ✅ Edit button
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, addmenuitem.class);
            intent.putExtra("editItemId", item.getId());
            context.startActivity(intent);
        });

        return convertView;
    }
}
