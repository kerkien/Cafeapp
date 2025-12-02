package com.example.cafeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuItemAdapter extends BaseAdapter {

    private Context context;
    private List<MenuItem> menuItems;
    private LayoutInflater inflater;
    private Set<MenuItem> selectedItems = new HashSet<>();
    private boolean isCustomerView;
    private FirebaseFirestore db;

    public MenuItemAdapter(Context context, List<MenuItem> menuItems, boolean isCustomerView) {
        this.context = context;
        this.menuItems = menuItems;
        this.isCustomerView = isCustomerView;
        this.inflater = LayoutInflater.from(context);
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getCount() { return menuItems.size(); }

    @Override
    public Object getItem(int position) { return menuItems.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.menu_item_layout, parent, false);
        }

        ImageView imgMenuItem = convertView.findViewById(R.id.imgMenuItem);
        TextView txtName = convertView.findViewById(R.id.txtName);
        TextView txtCategory = convertView.findViewById(R.id.txtCategory);
        TextView txtPrice = convertView.findViewById(R.id.txtPrice);
        CheckBox checkBox = convertView.findViewById(R.id.checkbox_select);
        ImageButton btnEdit = convertView.findViewById(R.id.btnEdit);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        MenuItem item = menuItems.get(position);

        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText("$" + item.getPrice());

        String imageBase64 = item.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imgMenuItem.setImageBitmap(bitmap);
            } catch (Exception e) {
                imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
        }

        if (isCustomerView) {
            checkBox.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(selectedItems.contains(item));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedItems.add(item);
                } else {
                    selectedItems.remove(item);
                }
            });
        } else {
            checkBox.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, addmenuitem.class);
                intent.putExtra("editItemId", item.getId());
                context.startActivity(intent);
            });

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete '" + item.getName() + "'?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("menuItems").document(item.getId()).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        return convertView;
    }

    public List<MenuItem> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }
}