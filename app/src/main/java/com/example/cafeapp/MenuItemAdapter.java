package com.example.cafeapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuItemAdapter extends BaseAdapter {
    private static final String TAG = "MenuAdapter";

    private Context context;
    private ArrayList<MenuItem> menuItems;
    private boolean isCustomerView;

    // Cart system - map item ID to quantity
    private Map<String, Integer> cart = new HashMap<>();

    public MenuItemAdapter(Context context, ArrayList<MenuItem> menuItems, boolean isCustomerView) {
        this.context = context;
        this.menuItems = menuItems;
        this.isCustomerView = isCustomerView;
        Log.d(TAG, "Adapter created - isCustomerView: " + isCustomerView + ", items count: " + menuItems.size());
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.menu_item_layout, parent, false);
            holder = new ViewHolder();
            holder.imgMenuItem = convertView.findViewById(R.id.imgMenuItem);
            holder.txtName = convertView.findViewById(R.id.txtName);
            holder.txtCategory = convertView.findViewById(R.id.txtCategory);
            holder.txtPrice = convertView.findViewById(R.id.txtPrice);
            holder.checkboxSelect = convertView.findViewById(R.id.checkbox_select);
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            holder.layoutQuantity = convertView.findViewById(R.id.layoutQuantity);
            holder.btnDecrease = convertView.findViewById(R.id.btnDecrease);
            holder.btnIncrease = convertView.findViewById(R.id.btnIncrease);
            holder.txtQuantity = convertView.findViewById(R.id.txtQuantity);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MenuItem item = menuItems.get(position);

        // Set item data
        holder.txtName.setText(item.getName());
        holder.txtCategory.setText(item.getDescription() != null ? item.getDescription() : item.getCategory());
        holder.txtPrice.setText(String.format("$%.2f", item.getPrice()));

        // Load image
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(item.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imgMenuItem.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode image for item: " + item.getName(), e);
                holder.imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            holder.imgMenuItem.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Show/hide appropriate controls based on view type
        if (isCustomerView) {
            Log.d(TAG, "Customer view - showing quantity controls for: " + item.getName());
            // Customer view: show quantity controls, hide edit/delete
            holder.checkboxSelect.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.layoutQuantity.setVisibility(View.VISIBLE);

            // Get current quantity from cart
            int currentQuantity = cart.getOrDefault(item.getId(), 0);
            holder.txtQuantity.setText(String.valueOf(currentQuantity));

            // Decrease button
            holder.btnDecrease.setOnClickListener(v -> {
                int qty = cart.getOrDefault(item.getId(), 0);
                if (qty > 0) {
                    qty--;
                    if (qty == 0) {
                        cart.remove(item.getId());
                    } else {
                        cart.put(item.getId(), qty);
                    }
                    holder.txtQuantity.setText(String.valueOf(qty));
                    Log.d(TAG, "Decreased " + item.getName() + " to " + qty);
                }
            });

            // Increase button
            holder.btnIncrease.setOnClickListener(v -> {
                int qty = cart.getOrDefault(item.getId(), 0);
                qty++;
                cart.put(item.getId(), qty);
                holder.txtQuantity.setText(String.valueOf(qty));
                Log.d(TAG, "Increased " + item.getName() + " to " + qty);
            });

        } else {
            Log.d(TAG, "Admin view - showing edit/delete for: " + item.getName());
            // Admin view: hide quantity controls, show edit/delete
            holder.checkboxSelect.setVisibility(View.GONE);
            holder.layoutQuantity.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            // Edit button
            holder.btnEdit.setOnClickListener(v -> {
                Log.d(TAG, "Edit button clicked for item: " + item.getName() + ", ID: " + item.getId());
                Log.d(TAG, "Context type: " + context.getClass().getName());

                if (context instanceof MenuForAdminActivity) {
                    Log.d(TAG, "Calling editMenuItem on MenuForAdminActivity");
                    ((MenuForAdminActivity) context).editMenuItem(item);
                } else {
                    Log.e(TAG, "Context is NOT MenuForAdminActivity!");
                }
            });

            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete button clicked for item: " + item.getName() + ", ID: " + item.getId());

                if (context instanceof MenuForAdminActivity) {
                    Log.d(TAG, "Calling deleteMenuItem on MenuForAdminActivity");
                    ((MenuForAdminActivity) context).deleteMenuItem(item);
                } else {
                    Log.e(TAG, "Context is NOT MenuForAdminActivity!");
                }
            });
        }

        return convertView;
    }

    // Get cart items with quantities
    public List<CartItem> getCartItems() {
        List<CartItem> cartItems = new ArrayList<>();
        for (MenuItem item : menuItems) {
            Integer quantity = cart.get(item.getId());
            if (quantity != null && quantity > 0) {
                cartItems.add(new CartItem(item, quantity));
            }
        }
        Log.d(TAG, "getCartItems called, returning " + cartItems.size() + " items");
        return cartItems;
    }

    // Get total items count in cart
    public int getCartItemCount() {
        int total = 0;
        for (Integer qty : cart.values()) {
            total += qty;
        }
        return total;
    }

    // Get total price
    public double getCartTotal() {
        double total = 0.0;
        for (MenuItem item : menuItems) {
            Integer quantity = cart.get(item.getId());
            if (quantity != null && quantity > 0) {
                total += item.getPrice() * quantity;
            }
        }
        return total;
    }

    // Clear cart
    public void clearCart() {
        Log.d(TAG, "clearCart called");
        cart.clear();
        notifyDataSetChanged();
    }

    // Keep for backward compatibility (deprecated)
    @Deprecated
    public List<MenuItem> getSelectedItems() {
        List<MenuItem> selected = new ArrayList<>();
        for (CartItem cartItem : getCartItems()) {
            selected.add(cartItem.getMenuItem());
        }
        return selected;
    }

    static class ViewHolder {
        ImageView imgMenuItem;
        TextView txtName;
        TextView txtCategory;
        TextView txtPrice;
        CheckBox checkboxSelect;
        ImageButton btnEdit;
        ImageButton btnDelete;
        LinearLayout layoutQuantity;
        ImageButton btnDecrease;
        ImageButton btnIncrease;
        TextView txtQuantity;
    }
}