package com.shivam.sfl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ImportCollectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            Toast.makeText(this, "No data found in link", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if ("location".equals(uri.getHost())) {
            SavedLocationEntity loc = LocationShareLink.parseLink(uri);
            if (loc == null) {
                Toast.makeText(this, "Invalid SFL location QR code or link", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            showImportLocationDialog(loc);
        } else if ("collection".equals(uri.getHost())) {
            CollectionShareLink.SharedCollection shared = CollectionShareLink.parseLink(uri);
            if (shared == null || shared.locations.isEmpty()) {
                Toast.makeText(this, "This link isn't a valid SFL collection", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            showImportCollectionDialog(shared);
        } else {
            Toast.makeText(this, "Unrecognized SFL link", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showImportLocationDialog(SavedLocationEntity loc) {
        String msg = "Name: " + loc.getName()
                + "\nAddress: " + loc.getAddress()
                + (loc.getPlusCode() != null ? "\nPlus Code: " + loc.getPlusCode() : "")
                + "\nType: " + loc.getType();

        new MaterialAlertDialogBuilder(this)
            .setTitle("Save Location \"" + loc.getName() + "\"?")
            .setMessage(msg)
            .setPositiveButton("SAVE LOCATION", (dialog, which) -> {
                DatabaseHandler db = new DatabaseHandler(this);
                db.addLocation(loc);
                Toast.makeText(this, "Saved \"" + loc.getName() + "\"", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SavedLocationList.class));
                finish();
            })
            .setNegativeButton("CANCEL", (dialog, which) -> finish())
            .setOnCancelListener(dialog -> finish())
            .show();
    }

    private void showImportCollectionDialog(CollectionShareLink.SharedCollection shared) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Import \"" + shared.name + "\"?")
            .setMessage("This adds " + shared.locations.size() + " location(s) to your saved places in a new collection called \""
                    + shared.name + "\".")
            .setPositiveButton("IMPORT", (dialog, which) -> {
                importCollection(shared);
                Toast.makeText(this, "Imported \"" + shared.name + "\"", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CollectionsActivity.class));
                finish();
            })
            .setNegativeButton("CANCEL", (dialog, which) -> finish())
            .setOnCancelListener(dialog -> finish())
            .show();
    }

    private void importCollection(CollectionShareLink.SharedCollection shared) {
        DatabaseHandler db = new DatabaseHandler(this);
        long collectionId = db.getOrCreateCollection(shared.name);
        for (SavedLocationEntity loc : shared.locations) {
            long newId = db.insertLocation(loc);
            if (newId != -1) db.addLocationToCollection((int) newId, (int) collectionId);
        }
    }
}
