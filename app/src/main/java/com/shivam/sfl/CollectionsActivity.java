package com.shivam.sfl;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class CollectionsActivity extends AppCompatActivity {
    private static final int QR_MAX_LINK_LENGTH = 900;

    private BottomNavigationView bottomNavigationView;
    private DatabaseHandler mDatabaseHandler;
    private RecyclerView collectionsList;
    private View emptyState;
    private List<CollectionEntity> collections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        setSupportActionBar(findViewById(R.id.toolbar));

        mDatabaseHandler = new DatabaseHandler(this);
        collectionsList = findViewById(R.id.collections_list);
        emptyState = findViewById(R.id.empty_state);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_collection);
        fabAdd.setOnClickListener(v -> promptNewCollection());

        setupNavigation();
        refreshList();
    }

    private void refreshList() {
        collections = mDatabaseHandler.getAllCollections();
        CollectionListAdapter adapter = new CollectionListAdapter(collections, new CollectionListAdapter.Listener() {
            @Override
            public void onCollectionClick(CollectionEntity collection) {
                Intent intent = new Intent(CollectionsActivity.this, SavedLocationList.class);
                intent.putExtra("collection_id", collection.getId());
                intent.putExtra("collection_name", collection.getName());
                startActivity(intent);
            }

            @Override
            public void onCollectionDelete(CollectionEntity collection) {
                new AlertDialog.Builder(CollectionsActivity.this)
                    .setTitle("Delete collection")
                    .setMessage("Delete \"" + collection.getName() + "\"? The locations in it won't be deleted.")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        mDatabaseHandler.deleteCollection(collection.getId());
                        refreshList();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            }

            @Override
            public void onCollectionShare(CollectionEntity collection) {
                shareCollection(collection);
            }
        });
        collectionsList.setLayoutManager(new LinearLayoutManager(this));
        collectionsList.setAdapter(adapter);

        boolean isEmpty = collections.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        collectionsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void shareCollection(CollectionEntity collection) {
        List<SavedLocationEntity> locations = mDatabaseHandler.getLocationsInCollection(collection.getId());
        if (locations.isEmpty()) {
            Toast.makeText(this, "Add some locations to this collection first", Toast.LENGTH_SHORT).show();
            return;
        }
        String link = CollectionShareLink.buildLink(collection.getName(), locations);
        if (link == null) {
            Toast.makeText(this, "Couldn't build a share link", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_share_collection, null);
        ImageView ivQr = dialogView.findViewById(R.id.iv_qr_code);
        TextView tvUnavailable = dialogView.findViewById(R.id.tv_qr_unavailable);

        Bitmap qr = link.length() <= QR_MAX_LINK_LENGTH ? QrCodeGenerator.generate(link, 600) : null;
        if (qr != null) {
            ivQr.setImageBitmap(qr);
        } else {
            ivQr.setVisibility(View.GONE);
            tvUnavailable.setVisibility(View.VISIBLE);
        }

        dialogView.findViewById(R.id.btn_copy_link).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("SFL Collection Link", link));
            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
        });
        dialogView.findViewById(R.id.btn_share_link).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SFL Collection: " + collection.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my \"" + collection.getName() + "\" collection on SFL: " + link
                    + "\n\n(You'll need the SFL app installed to open this link.)");
            startActivity(Intent.createChooser(shareIntent, "Share Collection"));
        });

        new AlertDialog.Builder(this)
            .setTitle("Share \"" + collection.getName() + "\"")
            .setView(dialogView)
            .setNegativeButton("CLOSE", null)
            .show();
    }

    private void promptNewCollection() {
        EditText input = new EditText(this);
        input.setHint("Collection name");
        new AlertDialog.Builder(this)
            .setTitle("New collection")
            .setView(input)
            .setPositiveButton("CREATE", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                mDatabaseHandler.getOrCreateCollection(name);
                refreshList();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNav_view);
        bottomNavigationView.setSelectedItemId(R.id.nav_collections);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_explore) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_saved) {
                startActivity(new Intent(this, SavedLocationList.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_collections) {
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });

        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        BottomNavigationMenuView bottomNavigationMenuView = (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        View v = bottomNavigationMenuView.getChildAt(1);
        if (!(v instanceof BottomNavigationItemView itemView)) return;

        View existingBadge = itemView.findViewById(R.id.notifications_badge);
        if (existingBadge != null) itemView.removeView(existingBadge);

        LayoutInflater.from(this).inflate(R.layout.notification_badge, (ViewGroup) itemView, true);
        TextView notificationBadge = itemView.findViewById(R.id.notifications_badge);
        int count = mDatabaseHandler.getAllLocations().size();
        notificationBadge.setText(String.valueOf(count));
        notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.getMenu().findItem(R.id.nav_collections).setChecked(true);
        refreshList();
        updateNotificationBadge();
    }
}
