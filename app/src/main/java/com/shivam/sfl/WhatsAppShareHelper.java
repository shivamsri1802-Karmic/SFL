package com.shivam.sfl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WhatsAppShareHelper {

    public static void showWhatsAppShareDialog(Context context, @Nullable SavedLocationEntity preselectedLocation) {
        DatabaseHandler db = new DatabaseHandler(context);
        List<SavedLocationEntity> allLocations = db.getAllLocations();

        if (allLocations.isEmpty()) {
            Toast.makeText(context, "No saved locations to share. Save a location first!", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_whatsapp_share, null);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);
        AutoCompleteTextView actvLocationPicker = dialogView.findViewById(R.id.actv_location_picker);
        TextInputLayout tilPhone = dialogView.findViewById(R.id.til_phone_number);

        List<String> locationNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < allLocations.size(); i++) {
            SavedLocationEntity loc = allLocations.get(i);
            locationNames.add(loc.getName() + " (" + loc.getAddress() + ")");
            if (preselectedLocation != null && loc.getID() == preselectedLocation.getID()) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, locationNames);
        actvLocationPicker.setAdapter(adapter);
        actvLocationPicker.setText(locationNames.get(selectedIndex), false);

        final int[] finalSelectedIndex = {selectedIndex};
        actvLocationPicker.setOnItemClickListener((parent, view, position, id) -> finalSelectedIndex[0] = position);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_send_whatsapp).setOnClickListener(v -> {
            String rawPhone = etPhoneNumber.getText() != null ? etPhoneNumber.getText().toString().trim() : "";
            String cleanedPhone = cleanPhoneNumber(rawPhone);

            if (cleanedPhone.length() < 7) {
                tilPhone.setError("Enter a valid phone number with country code (e.g. 14155552671)");
                return;
            }
            tilPhone.setError(null);

            SavedLocationEntity targetLocation = allLocations.get(finalSelectedIndex[0]);
            sendWhatsAppMessage(context, cleanedPhone, targetLocation);
            dialog.dismiss();
        });

        dialog.show();
    }

    public static String cleanPhoneNumber(String input) {
        if (input == null) return "";
        return input.replaceAll("[^0-9]", "");
    }

    private static void sendWhatsAppMessage(Context context, String cleanedPhone, SavedLocationEntity loc) {
        try {
            String link = LocationShareLink.buildLink(loc);
            StringBuilder sb = new StringBuilder();
            sb.append("📍 *").append(loc.getName()).append("*\n");
            if (loc.getAddress() != null && !loc.getAddress().isEmpty()) {
                sb.append("Address: ").append(loc.getAddress()).append("\n");
            }
            sb.append("Google Maps: http://maps.google.com/maps?q=").append(loc.getLat()).append(",").append(loc.getLongt()).append("\n");
            if (loc.getPlusCode() != null && !loc.getPlusCode().isEmpty()) {
                sb.append("Plus Code: ").append(loc.getPlusCode()).append("\n");
            }
            if (link != null) {
                sb.append("Open in SFL: ").append(link);
            }

            String encodedMsg = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8.name());
            String whatsappUrl = "https://api.whatsapp.com/send?phone=" + cleanedPhone + "&text=" + encodedMsg;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to open WhatsApp: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
