package com.shivam.sfl;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.openlocationcode.OpenLocationCode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CompleteSaveLocation extends AppCompatActivity {
    private static final String TAG = "SFL_CompleteSaveLocation";
    private final int PICKFILE_REQUEST_CODE = 5;
    private final int PICK_CONTACT_REQUEST_CODE = 6;

    private TextView address;
    private BottomNavigationView bottomNavigationView;
    private TextView co_ordinates;
    private TextView id;
    private DatabaseHandler mDatabaseHandler;
    private SavedLocationEntity mSavedLocationEntity;
    private AutoCompleteTextView mSpinner;
    private EditText title;
    private MaterialSwitch switchSetHome;
    private MaterialSwitch switchSetWork;
    private TextView tvLinkedContact;
    private View btnRemoveContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_save_location);

        setSupportActionBar(findViewById(R.id.toolbar));

        this.mSpinner = findViewById(R.id.et_type);
        this.address = findViewById(R.id.address);
        this.co_ordinates = findViewById(R.id.co_ordinates);
        this.title = findViewById(R.id.title);
        this.id = findViewById(R.id.id);
        this.switchSetHome = findViewById(R.id.switch_set_home);
        this.switchSetWork = findViewById(R.id.switch_set_work);
        this.tvLinkedContact = findViewById(R.id.tv_linked_contact);
        this.btnRemoveContact = findViewById(R.id.btn_remove_contact);

        String[] types = getResources().getStringArray(R.array.types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        mSpinner.setAdapter(adapter);

        this.mSavedLocationEntity = new SavedLocationEntity();
        this.mDatabaseHandler = new DatabaseHandler(this);

        // Loaded once here (not onResume) since onActivityResult from the contact
        // picker triggers onResume right after - re-reading these stale intent
        // extras there would clobber the contact the user just picked.
        mSavedLocationEntity.setContactId(getIntent().getStringExtra("contact_id"));
        mSavedLocationEntity.setContactName(getIntent().getStringExtra("contact_name"));
        updateContactUi();

        findViewById(R.id.btn_link_contact).setOnClickListener(v -> pickContact());
        btnRemoveContact.setOnClickListener(v -> {
            mSavedLocationEntity.setContactId(null);
            mSavedLocationEntity.setContactName(null);
            updateContactUi();
        });

        setupNavigation();
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST_CODE);
    }

    private void handleContactPicked(Uri contactUri) {
        try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                mSavedLocationEntity.setContactId(contactUri.toString());
                mSavedLocationEntity.setContactName(displayName);
                updateContactUi();
            }
        }
    }

    private void updateContactUi() {
        String name = mSavedLocationEntity.getContactName();
        if (name != null && !name.isEmpty()) {
            tvLinkedContact.setText("Linked to: " + name);
            btnRemoveContact.setVisibility(View.VISIBLE);
        } else {
            tvLinkedContact.setText("No contact linked");
            btnRemoveContact.setVisibility(View.GONE);
        }
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNav_view);
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
        if (v instanceof BottomNavigationItemView itemView) {
            View existingBadge = itemView.findViewById(R.id.notifications_badge);
            if (existingBadge != null) {
                itemView.removeView(existingBadge);
            }
            LayoutInflater.from(this).inflate(R.layout.notification_badge, (ViewGroup) itemView, true);
            TextView notificationBadge = itemView.findViewById(R.id.notifications_badge);
            int count = mDatabaseHandler.getAllLocations().size();
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getStringExtra("name") != null) {
            this.title.setText(getIntent().getStringExtra("name"));
        }
        if (getIntent().getStringExtra("type") != null) {
            this.mSpinner.setText(getIntent().getStringExtra("type"));
        }
        if (getIntent().getIntExtra("id", 0) != 0) {
            this.id.setText(String.valueOf(getIntent().getIntExtra("id", 0)));
        }
        this.address.setText(getIntent().getStringExtra("address"));
        this.co_ordinates.setText(getIntent().getStringExtra("co-ordinates"));

        int currentId = getIntent().getIntExtra("id", 0);
        switchSetHome.setChecked(currentId != 0 && QuickSetLocations.getHomeLocationId(this) == currentId);
        switchSetWork.setChecked(currentId != 0 && QuickSetLocations.getWorkLocationId(this) == currentId);
    }

    public void saveLocation(View view) {
        String enteredName = this.title.getText().toString().trim();
        if (enteredName.isEmpty()) {
            this.title.setError("Name is required");
            return;
        }
        
        mSavedLocationEntity.setName(enteredName);
        mSavedLocationEntity.setType(mSpinner.getText().toString());
        
        String addrFull = address.getText().toString();
        if (addrFull.contains("\nPlus Code: ")) {
            String[] parts = addrFull.split("\nPlus Code: ");
            mSavedLocationEntity.setAddress(parts[0]);
            mSavedLocationEntity.setPlusCode(parts[1]);
        } else {
            mSavedLocationEntity.setAddress(addrFull);
        }
        
        String coords = co_ordinates.getText().toString();
        if (coords.contains(",")) {
            String[] parts = coords.split(",");
            double latVal = Double.parseDouble(parts[0].trim());
            double lonVal = Double.parseDouble(parts[1].trim());
            mSavedLocationEntity.setLat(latVal);
            mSavedLocationEntity.setLongt(lonVal);
            if (mSavedLocationEntity.getPlusCode() == null) {
                mSavedLocationEntity.setPlusCode(OpenLocationCode.encode(latVal, lonVal));
            }
        }
        
        mSavedLocationEntity.setTimeStamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        
        if (!id.getText().toString().isEmpty()) {
            mSavedLocationEntity.setID(Integer.parseInt(id.getText().toString()));
        }

        int entityId = mSavedLocationEntity.getID();
        if (switchSetHome.isChecked()) {
            QuickSetLocations.setHomeLocationId(this, entityId);
        } else {
            QuickSetLocations.clearHomeIfMatches(this, entityId);
        }
        if (switchSetWork.isChecked()) {
            QuickSetLocations.setWorkLocationId(this, entityId);
        } else {
            QuickSetLocations.clearWorkIfMatches(this, entityId);
        }

        AppExecutors.getInstance().runOnBackground(() -> {
            mDatabaseHandler.addLocation(mSavedLocationEntity);
            AppExecutors.getInstance().runOnMainThread(() -> {
                Toast.makeText(this, "Location Saved", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SavedLocationList.class));
                finish();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.export_data) {
            LocationImportAndExport.exportLocation(this);
            return true;
        } else if (itemId == R.id.import_data) {
            importLocation();
            return true;
        }
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
        if (requestCode == PICK_CONTACT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                handleContactPicked(data.getData());
            }
            return;
        }
        LocationImportAndExport.importLocation(requestCode, resultCode, data, this);
    }
}
