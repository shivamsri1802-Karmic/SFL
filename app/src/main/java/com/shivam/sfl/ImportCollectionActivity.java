package com.shivam.sfl;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ImportCollectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        CollectionShareLink.SharedCollection shared = CollectionShareLink.parseLink(uri);

        if (shared == null || shared.locations.isEmpty()) {
            Toast.makeText(this, "This link isn't a valid SFL collection", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Import \"" + shared.name + "\"?")
            .setMessage("This adds " + shared.locations.size() + " location(s) to your saved places in a new collection called \""
                    + shared.name + "\". Locations are added as new entries, even if similar ones already exist.")
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
