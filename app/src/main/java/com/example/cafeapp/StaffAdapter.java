package com.example.cafeapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class StaffAdapter extends ArrayAdapter<Staff> {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public StaffAdapter(Context context, List<Staff> staffList) {
        super(context, 0, staffList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.staff_list_item, parent, false);
        }

        Staff staffMember = getItem(position);

        TextView txtEmail = convertView.findViewById(R.id.txtStaffEmail);
        TextView txtRole = convertView.findViewById(R.id.txtStaffRole);
        SwitchCompat switchActive = convertView.findViewById(R.id.switchStaffActive);

        if (staffMember != null) {
            txtEmail.setText(staffMember.getEmail());
            txtRole.setText(staffMember.getRole());

            // Set initial checked state without triggering the listener
            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(staffMember.isActive());

            // Now, set the listener to update Firestore when the user toggles the switch
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                db.collection("users").document(staffMember.getUid()).update("active", isChecked);
            });
        }

        return convertView;
    }
}