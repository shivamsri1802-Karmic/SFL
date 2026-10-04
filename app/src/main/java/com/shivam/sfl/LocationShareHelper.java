package com.shivam.sfl;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LocationShareHelper {

    public static void showShareDialog(Context context, SavedLocationEntity location) {
        String link = LocationShareLink.buildLink(location);
        if (link == null) {
            Toast.makeText(context, "Error creating share link", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_share_location_qr, null);
        ImageView ivQr = dialogView.findViewById(R.id.iv_qr_code);
        TextView tvTitle = dialogView.findViewById(R.id.tv_location_title);
        TextView tvAddress = dialogView.findViewById(R.id.tv_location_address);

        tvTitle.setText(location.getName());
        tvAddress.setText(location.getAddress());

        final Bitmap qrBitmap = QrCodeGenerator.generate(link, 600);
        if (qrBitmap != null) {
            ivQr.setImageBitmap(qrBitmap);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Share \"" + location.getName() + "\"")
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_copy_link).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("SFL Location Link", link));
                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btn_share_text).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            String shareBody = "Name: " + location.getName()
                    + "\nCoordinates: http://maps.google.com/maps?q=" + location.getLat() + "," + location.getLongt()
                    + "\nAddress: " + location.getAddress()
                    + (location.getPlusCode() != null ? "\nPlus Code: " + location.getPlusCode() : "")
                    + "\nOpen in SFL: " + link;
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Location: " + location.getName());
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
            context.startActivity(Intent.createChooser(intent, "Share Location"));
        });

        dialogView.findViewById(R.id.btn_share_qr_image).setOnClickListener(v -> {
            if (qrBitmap != null) {
                shareQrImage(context, qrBitmap, location.getName());
            } else {
                Toast.makeText(context, "QR code image unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private static void shareQrImage(Context context, Bitmap bitmap, String name) {
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            File imageFile = new File(cachePath, "qr_location.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri imageUri = FileProvider.getUriForFile(context, context.getPackageName(), imageFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code for " + name);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code Image"));
        } catch (IOException e) {
            Toast.makeText(context, "Failed to share QR image", Toast.LENGTH_SHORT).show();
        }
    }
}
