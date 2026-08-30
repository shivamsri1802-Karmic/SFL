package com.shivam.sfl;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.openlocationcode.OpenLocationCode;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "SFL_MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int PICKFILE_REQUEST_CODE = 5;
    private static final int TURNONGPS_REQUEST_CODE = 6;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabSaveLocation;
    private View ivCenterPin;
    private DatabaseHandler mDatabaseHandler;
    private GoogleMap mMap;
    private final Map<Integer, BitmapDescriptor> mMarkerIconCache = new HashMap<>();
    private final Map<Integer, Marker> mMarkersByLocationId = new HashMap<>();

    // Location capturing
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Geocoder mGeocoder;
    private Location mCapturedLocation;
    private BottomSheetBehavior<View> saveBottomSheetBehavior;
    private boolean isAdjusting = false;
    // Bumped on every pin move; lets a slow/out-of-order background geocode result
    // recognize it's stale and avoid overwriting a newer one (see updateCapturedInfo).
    private int captureRequestId = 0;

    // Bottom Sheet Views
    private EditText etPlaceName;
    private AutoCompleteTextView etType;
    private TextView tvCurrentAddress;
    private Button btnSave;
    private Button btnConfirmSpot;
    private View tilName, tilType, tvDragHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabaseHandler = new DatabaseHandler(this);
        mGeocoder = new Geocoder(this, Locale.getDefault());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        ivCenterPin = findViewById(R.id.iv_center_pin);

        setupNavigation();
        setupMap();
        setupFab();
        setupBottomSheet();
        setupQuickAccess();

        handleAutoCaptureIntent(getIntent());
    }

    private void setupQuickAccess() {
        findViewById(R.id.btn_go_home).setOnClickListener(v -> goToQuickSetLocation(QuickSetLocations.getHomeLocationId(this), "Home"));
        findViewById(R.id.btn_go_work).setOnClickListener(v -> goToQuickSetLocation(QuickSetLocations.getWorkLocationId(this), "Work"));
    }

    private void goToQuickSetLocation(int locationId, String label) {
        if (locationId == -1) {
            Toast.makeText(this, label + " isn't set yet — edit a saved location and toggle \"Set as " + label + "\"", Toast.LENGTH_LONG).show();
            return;
        }
        SavedLocationEntity location = mDatabaseHandler.getLocationById(locationId);
        if (location == null) {
            Toast.makeText(this, label + " location no longer exists", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLat(), location.getLongt()), 16f));
            Marker marker = mMarkersByLocationId.get(locationId);
            if (marker != null) marker.showInfoWindow();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAutoCaptureIntent(intent);
    }

    private void handleAutoCaptureIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(SaveLocationWidgetProvider.EXTRA_AUTO_CAPTURE, false)) {
            intent.removeExtra(SaveLocationWidgetProvider.EXTRA_AUTO_CAPTURE);
            startQuickCapture();
        }
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNav_view);
        bottomNavigationView.setSelectedItemId(R.id.nav_explore);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isAdjusting) cancelAdjusting();
            int itemId = item.getItemId();
            if (itemId == R.id.nav_explore) return true;
            if (itemId == R.id.nav_saved) {
                startActivity(new Intent(this, SavedLocationList.class));
                return true;
            }
            if (itemId == R.id.nav_collections) {
                startActivity(new Intent(this, CollectionsActivity.class));
                return true;
            }
            if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
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

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void setupFab() {
        fabSaveLocation = findViewById(R.id.fab_save_location);
        fabSaveLocation.setOnClickListener(v -> startQuickCapture());
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet_save);
        saveBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        saveBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        saveBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) cancelAdjusting();
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        etPlaceName = findViewById(R.id.et_place_name);
        etType = findViewById(R.id.et_type);
        tvCurrentAddress = findViewById(R.id.tv_current_address);
        btnSave = findViewById(R.id.btn_save);
        btnConfirmSpot = findViewById(R.id.btn_confirm_spot);
        tilName = findViewById(R.id.til_name);
        tilType = findViewById(R.id.til_type);
        tvDragHint = findViewById(R.id.tv_drag_hint);

        String[] types = getResources().getStringArray(R.array.types);
        etType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types));

        btnConfirmSpot.setOnClickListener(v -> {
            btnConfirmSpot.setVisibility(View.GONE);
            tvDragHint.setVisibility(View.GONE);
            tilName.setVisibility(View.VISIBLE);
            tilType.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);
            saveBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        btnSave.setOnClickListener(v -> saveCapturedLocation());
    }

    private void cancelAdjusting() {
        isAdjusting = false;
        ivCenterPin.setVisibility(View.GONE);
        fabSaveLocation.show();
        saveBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        
        btnConfirmSpot.setVisibility(View.VISIBLE);
        tvDragHint.setVisibility(View.VISIBLE);
        tilName.setVisibility(View.GONE);
        tilType.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        
        loadSavedLocationsOnMap();
    }

    private void startQuickCapture() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }
        if (!LocationImportAndExport.checkGpsStatus(this)) {
            startActivityForResult(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"), TURNONGPS_REQUEST_CODE);
            return;
        }

        isAdjusting = true;
        fabSaveLocation.hide();
        ivCenterPin.setVisibility(View.VISIBLE);
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true).setMaxUpdates(1).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    mCapturedLocation = location;
                    // Stop updates immediately so the user can drag the map
                    fusedLocationClient.removeLocationUpdates(this);
                    
                    if (mMap != null) {
                        mMap.clear(); // Remove saved markers while adjusting
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 18f));
                    }
                    updateCapturedInfo(location.getLatitude(), location.getLongitude());
                    saveBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateCapturedInfo(double lat, double lng) {
        if (mCapturedLocation == null) mCapturedLocation = new Location("manual");
        mCapturedLocation.setLatitude(lat);
        mCapturedLocation.setLongitude(lng);

        // Dragging/zooming the map while adjusting the pin can fire this several times in
        // quick succession. They all run on the same background thread, but the OS gives no
        // guarantee they *finish* in the order they started - a slow geocode for an earlier
        // pin position could otherwise land after (and overwrite) a faster one for a later
        // position, showing the wrong address for where the pin actually is. Tag each request
        // with a generation number and drop any result that isn't for the latest one.
        final int requestId = ++captureRequestId;
        AppExecutors.getInstance().runOnBackground(() -> {
            String addressText = "Searching address...";
            String plusCode = OpenLocationCode.encode(lat, lng);
            try {
                List<Address> addresses = mGeocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) addressText = addresses.get(0).getAddressLine(0);
            } catch (IOException e) { Log.e(TAG, "Geocoding failed", e); }

            final String finalAddress = addressText;
            final String finalPlusCode = plusCode;
            AppExecutors.getInstance().runOnMainThread(() -> {
                if (requestId != captureRequestId) return; // a newer pin position has since been geocoded
                tvCurrentAddress.setText(finalAddress + "\nPlus Code: " + finalPlusCode);
            });
        });
    }

    private void saveCapturedLocation() {
        if (mCapturedLocation == null) return;
        String name = etPlaceName.getText().toString().trim();
        if (name.isEmpty()) { etPlaceName.setError("Name is required"); return; }

        SavedLocationEntity entity = new SavedLocationEntity();
        entity.setName(name);
        entity.setType(etType.getText().toString());
        String uiText = tvCurrentAddress.getText().toString();
        if (uiText.contains("\nPlus Code: ")) {
            String[] parts = uiText.split("\nPlus Code: ");
            entity.setAddress(parts[0]);
            entity.setPlusCode(parts[1]);
        } else {
            entity.setAddress(uiText);
            entity.setPlusCode(OpenLocationCode.encode(mCapturedLocation.getLatitude(), mCapturedLocation.getLongitude()));
        }
        entity.setLat(mCapturedLocation.getLatitude());
        entity.setLongt(mCapturedLocation.getLongitude());
        entity.setTimeStamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

        mDatabaseHandler.addLocation(entity);
        cancelAdjusting();
        Snackbar.make(findViewById(R.id.map_container), "Location saved!", Snackbar.LENGTH_LONG).setAction("View", v -> startActivity(new Intent(this, SavedLocationList.class))).show();
        updateNotificationBadge();
    }

    private BitmapDescriptor getMarkerIcon(int colorResId) {
        BitmapDescriptor cached = mMarkerIconCache.get(colorResId);
        if (cached != null) return cached;

        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.ic_map_pin)).mutate();
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, colorResId));
        int size = (int) (36 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        mMarkerIconCache.put(colorResId, descriptor);
        return descriptor;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean styleApplied = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!styleApplied) Log.e(TAG, "Map style parsing failed");
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Map style resource not found", e);
        }
        mMap.setOnCameraIdleListener(() -> {
            if (isAdjusting && mMap != null) {
                LatLng center = mMap.getCameraPosition().target;
                updateCapturedInfo(center.latitude, center.longitude);
            }
        });
        if (checkPermission()) {
            try { mMap.setMyLocationEnabled(true); } catch (SecurityException e) { Log.e(TAG, "Permission error", e); }
        }
        loadSavedLocationsOnMap();
    }

    private void loadSavedLocationsOnMap() {
        if (mMap == null || isAdjusting) return;
        mMap.clear();
        mMarkersByLocationId.clear();
        List<SavedLocationEntity> locations = mDatabaseHandler.getAllLocations();
        if (locations.isEmpty()) {
            if (checkPermission()) fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null && !isAdjusting) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12f));
            });
            return;
        }
        int homeId = QuickSetLocations.getHomeLocationId(this);
        int workId = QuickSetLocations.getWorkLocationId(this);
        LatLng lastLoc = null;
        for (SavedLocationEntity loc : locations) {
            LatLng latLng = new LatLng(loc.getLat(), loc.getLongt());
            int colorResId = loc.getID() == homeId ? R.color.colorPrimary
                    : loc.getID() == workId ? R.color.marker_work_color
                    : R.color.colorSecondary;
            String title = loc.getID() == homeId ? "🏠 " + loc.getName()
                    : loc.getID() == workId ? "💼 " + loc.getName()
                    : loc.getName();
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(loc.getType()).icon(getMarkerIcon(colorResId)));
            if (marker != null) mMarkersByLocationId.put(loc.getID(), marker);
            lastLoc = latLng;
        }
        if (lastLoc != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLoc, 12f));
    }

    private boolean checkPermission() { return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    private void requestPermission() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE); }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startQuickCapture();
    }

    @Override protected void onResume() { super.onResume(); bottomNavigationView.getMenu().findItem(R.id.nav_explore).setChecked(true); loadSavedLocationsOnMap(); updateNotificationBadge(); }

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.menu_list, menu); return true; }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.export_data) { LocationImportAndExport.exportLocation(this); return true; }
        if (itemId == R.id.import_data) { importLocation(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void importLocation() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) LocationImportAndExport.importLocation(requestCode, resultCode, data, this);
        else if (requestCode == TURNONGPS_REQUEST_CODE && LocationImportAndExport.checkGpsStatus(this)) startQuickCapture();
    }
}
