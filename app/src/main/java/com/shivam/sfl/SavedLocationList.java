package com.shivam.sfl;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedLocationList extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private DatabaseHandler mDatabaseHandler;
    private RecyclerView mLocationList;
    private SavedLocationListAdapter mSavedLocationListAdapter;
    private List<SavedLocationEntity> mSavedLocationListList;
    private EditText searchText;
    private final int PICKFILE_REQUEST_CODE = 5;

    private FusedLocationProviderClient fusedLocationClient;
    private Location mCurrentLocation;
    private boolean sortNearest = false;
    private View emptyState;
    private ActionMode actionMode;
    private int scopedCollectionId = -1;
    private String scopedCollectionName;

    private final ActionMode.Callback selectionActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_saved_location_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_selected) {
                confirmAndDeleteSelected();
                return true;
            } else if (itemId == R.id.action_export_selected) {
                LocationImportAndExport.exportLocations(SavedLocationList.this, mSavedLocationListAdapter.getSelectedItems());
                return true;
            } else if (itemId == R.id.action_add_to_collection) {
                promptAddSelectedToCollection();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            mSavedLocationListAdapter.clearSelection();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_location_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scopedCollectionId = getIntent().getIntExtra("collection_id", -1);
        scopedCollectionName = getIntent().getStringExtra("collection_name");
        if (scopedCollectionId != -1) toolbar.setTitle(scopedCollectionName);

        this.mLocationList = findViewById(R.id.location_list);
        this.searchText = findViewById(R.id.search);
        this.emptyState = findViewById(R.id.empty_state);
        this.mDatabaseHandler = new DatabaseHandler(this);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupEmptyState();

        setupList();
        setupSearch();
        setupNavigation();
    }

    private void setupEmptyState() {
        TextView title = findViewById(R.id.tv_empty_title);
        TextView subtitle = findViewById(R.id.tv_empty_subtitle);
        Button cta = findViewById(R.id.btn_empty_state_go_to_map);
        if (scopedCollectionId != -1) {
            title.setText(R.string.empty_collection_locations_title);
            subtitle.setText(R.string.empty_collection_locations_subtitle);
            cta.setText(R.string.empty_collection_locations_cta);
            cta.setOnClickListener(v -> {
                startActivity(new Intent(this, SavedLocationList.class));
                finish();
            });
        } else {
            cta.setOnClickListener(v -> finish());
        }
    }

    private void setupList() {
        mSavedLocationListList = scopedCollectionId != -1
                ? mDatabaseHandler.getLocationsInCollection(scopedCollectionId)
                : mDatabaseHandler.getAllLocations();
        applySortAndBind();

        if (sortNearest && mCurrentLocation == null && checkPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location == null) return;
                mCurrentLocation = location;
                applySortAndBind();
            });
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void applySortAndBind() {
        if (sortNearest && mCurrentLocation != null) {
            for (SavedLocationEntity entity : mSavedLocationListList) {
                float[] results = new float[1];
                Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                        entity.getLat(), entity.getLongt(), results);
                entity.setDistanceMeters(results[0]);
            }
            Collections.sort(mSavedLocationListList, (o1, o2) -> Float.compare(o1.getDistanceMeters(), o2.getDistanceMeters()));
        } else {
            Collections.sort(mSavedLocationListList, (o1, o2) -> o2.getTimeStamp().compareTo(o1.getTimeStamp()));
        }

        mSavedLocationListAdapter = new SavedLocationListAdapter(mSavedLocationListList);
        mSavedLocationListAdapter.setOnDataChangedListener(this::updateEmptyState);
        mSavedLocationListAdapter.setSelectionListener(count -> {
            if (count == 0) {
                if (actionMode != null) actionMode.finish();
            } else {
                if (actionMode == null) actionMode = startSupportActionMode(selectionActionModeCallback);
                actionMode.setTitle(count + " selected");
            }
        });
        mLocationList.setLayoutManager(new LinearLayoutManager(this));
        mLocationList.setAdapter(mSavedLocationListAdapter);
        updateEmptyState();
    }

    private void confirmAndDeleteSelected() {
        List<SavedLocationEntity> selected = mSavedLocationListAdapter.getSelectedItems();
        new AlertDialog.Builder(this)
            .setTitle("Delete locations")
            .setMessage("Delete " + selected.size() + " selected location(s)? This can't be undone.")
            .setPositiveButton("DELETE", (dialog, which) -> {
                for (SavedLocationEntity entity : selected) {
                    mDatabaseHandler.deleteLocation(entity.getID());
                    QuickSetLocations.clearIfMatches(this, entity.getID());
                }
                mSavedLocationListAdapter.removeSelectedFromList();
                if (actionMode != null) actionMode.finish();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void promptAddSelectedToCollection() {
        List<SavedLocationEntity> selected = mSavedLocationListAdapter.getSelectedItems();
        List<CollectionEntity> existing = mDatabaseHandler.getAllCollections();
        List<String> options = new ArrayList<>();
        for (CollectionEntity c : existing) options.add(c.getName());
        options.add("+ New collection...");

        new AlertDialog.Builder(this)
            .setTitle("Add " + selected.size() + " location(s) to")
            .setItems(options.toArray(new String[0]), (dialog, which) -> {
                if (which == existing.size()) {
                    promptNewCollectionThenAssign(selected);
                } else {
                    assignToCollection(existing.get(which).getId(), selected);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void promptNewCollectionThenAssign(List<SavedLocationEntity> selected) {
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
                long collectionId = mDatabaseHandler.getOrCreateCollection(name);
                assignToCollection((int) collectionId, selected);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void assignToCollection(int collectionId, List<SavedLocationEntity> selected) {
        for (SavedLocationEntity entity : selected) {
            mDatabaseHandler.addLocationToCollection(entity.getID(), collectionId);
        }
        Toast.makeText(this, "Added to collection", Toast.LENGTH_SHORT).show();
        if (actionMode != null) actionMode.finish();
    }

    private void updateEmptyState() {
        boolean isEmpty = mSavedLocationListList.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mLocationList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        searchText.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupSearch() {
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSavedLocationListAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNav_view);
        bottomNavigationView.setSelectedItemId(scopedCollectionId != -1 ? R.id.nav_collections : R.id.nav_saved);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_explore) {
                finish(); // Go back to Map
                return true;
            } else if (itemId == R.id.nav_saved) {
                if (scopedCollectionId != -1) {
                    startActivity(new Intent(this, SavedLocationList.class));
                    finish();
                }
                return true;
            } else if (itemId == R.id.nav_collections) {
                startActivity(new Intent(this, CollectionsActivity.class));
                finish();
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
        if (!(v instanceof BottomNavigationItemView)) return;
        
        BottomNavigationItemView itemView = (BottomNavigationItemView) v;
        View existingBadge = itemView.findViewById(R.id.notifications_badge);
        if (existingBadge != null) {
            itemView.removeView(existingBadge);
        }

        LayoutInflater.from(this).inflate(R.layout.notification_badge, (ViewGroup) itemView, true);
        TextView notificationBadge = itemView.findViewById(R.id.notifications_badge);
        int count = mSavedLocationListList.size();
        notificationBadge.setText(String.valueOf(count));
        notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saved_location_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sortItem = menu.findItem(R.id.sort_toggle);
        if (sortItem != null) sortItem.setTitle(sortNearest ? "Sort: Most Recent" : "Sort: Nearest");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        bottomNavigationView.getMenu().findItem(scopedCollectionId != -1 ? R.id.nav_collections : R.id.nav_saved).setChecked(true);
        if (actionMode == null) setupList(); // Refresh - skipped mid-selection so returning from Export doesn't wipe it
        updateNotificationBadge();
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.export_data) {
            LocationImportAndExport.exportLocation(this);
            return true;
        } else if (itemId == R.id.import_data) {
            importLocation();
            return true;
        } else if (itemId == R.id.sort_toggle) {
            toggleSort();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSort() {
        sortNearest = !sortNearest;
        if (sortNearest && !checkPermission()) {
            Toast.makeText(this, "Location permission needed to sort by nearest", Toast.LENGTH_SHORT).show();
            sortNearest = false;
            return;
        }
        invalidateOptionsMenu();
        setupList();
    }

    private void importLocation() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            LocationImportAndExport.importLocation(requestCode, resultCode, data, this);
            setupList();
            updateNotificationBadge();
        }
    }
}
