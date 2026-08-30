package com.shivam.sfl;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private DatabaseHandler mDatabaseHandler;
    private SharedPreferences prefs;
    private final int PICKFILE_REQUEST_CODE = 5;
    private final int RC_SIGN_IN = 7;
    private final int RC_RECOVER_AUTH = 8;

    private MaterialSwitch switchCloudBackup;
    private TextView tvBackupStatus;
    private View backupActionsRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.toolbar));
        mDatabaseHandler = new DatabaseHandler(this);
        prefs = getSharedPreferences(SflApplication.PREFS_NAME, MODE_PRIVATE);

        setupThemeToggle();
        setupCloudBackup();
        setupDataActions();
        setupAbout();
        setupNavigation();
    }

    private void setupThemeToggle() {
        int lightId = R.id.btn_theme_light;
        int darkId = R.id.btn_theme_dark;
        int systemId = R.id.btn_theme_system;
        MaterialButtonToggleGroup group = findViewById(R.id.theme_toggle_group);

        String currentMode = prefs.getString(SflApplication.KEY_THEME_MODE, "system");
        group.check(currentMode.equals("light") ? lightId : currentMode.equals("dark") ? darkId : systemId);

        group.addOnButtonCheckedListener((toggleGroup, checkedId, isChecked) -> {
            if (!isChecked) return;
            String mode = checkedId == lightId ? "light" : checkedId == darkId ? "dark" : "system";
            prefs.edit().putString(SflApplication.KEY_THEME_MODE, mode).apply();
            SflApplication.applyNightMode(mode);
        });
    }

    private void setupCloudBackup() {
        switchCloudBackup = findViewById(R.id.switch_cloud_backup);
        tvBackupStatus = findViewById(R.id.tv_backup_status);
        backupActionsRow = findViewById(R.id.backup_actions_row);

        boolean signedIn = DriveBackupManager.getSignedInAccount(this) != null;
        boolean enabled = DriveBackupManager.isSyncEnabled(this) && signedIn;
        switchCloudBackup.setChecked(enabled);
        updateBackupStatusUi(enabled);

        switchCloudBackup.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                startActivityForResult(DriveBackupManager.getSignInClient(this).getSignInIntent(), RC_SIGN_IN);
            } else {
                DriveBackupManager.setSyncEnabled(this, false);
                DriveBackupManager.getSignInClient(this).signOut();
                updateBackupStatusUi(false);
            }
        });

        findViewById(R.id.btn_backup_now).setOnClickListener(v -> {
            tvBackupStatus.setText("Backing up…");
            DriveBackupManager.backupNow(this, mDatabaseHandler, backupCallback());
        });
        findViewById(R.id.btn_restore_backup).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Restore from Drive?")
                .setMessage("This adds every location from your last Drive backup to your saved places. It won't remove anything already on this device.")
                .setPositiveButton("RESTORE", (dialog, which) -> {
                    tvBackupStatus.setText("Restoring…");
                    DriveBackupManager.restoreNow(this, mDatabaseHandler, backupCallback());
                })
                .setNegativeButton("CANCEL", null)
                .show();
        });
    }

    private DriveBackupManager.Callback backupCallback() {
        return new DriveBackupManager.Callback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                updateBackupStatusUi(true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                updateBackupStatusUi(DriveBackupManager.isSyncEnabled(SettingsActivity.this));
            }

            @Override
            public void onRecoverableError(Intent recoveryIntent) {
                startActivityForResult(recoveryIntent, RC_RECOVER_AUTH);
            }
        };
    }

    private void updateBackupStatusUi(boolean enabled) {
        backupActionsRow.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            tvBackupStatus.setText("Off — your locations stay only on this device");
            return;
        }
        GoogleSignInAccount account = DriveBackupManager.getSignedInAccount(this);
        String email = account != null ? account.getEmail() : "your Google account";
        String lastSynced = DriveBackupManager.getLastSyncedText(this);
        tvBackupStatus.setText("Signed in as " + email
                + (lastSynced != null ? "\nLast synced: " + lastSynced : "\nNot backed up yet"));
    }

    private void setupDataActions() {
        findViewById(R.id.btn_export).setOnClickListener(v -> LocationImportAndExport.exportLocation(this));
        findViewById(R.id.btn_import).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICKFILE_REQUEST_CODE);
        });
    }

    private void setupAbout() {
        TextView versionText = findViewById(R.id.tv_app_version);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionText.setText(getString(R.string.app_name) + " v" + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText(R.string.app_name);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            LocationImportAndExport.importLocation(requestCode, resultCode, data, this);
        } else if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                DriveBackupManager.setSyncEnabled(this, true);
                updateBackupStatusUi(true);
                Toast.makeText(this, "Signed in as " + account.getEmail(), Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                switchCloudBackup.setChecked(false);
                Toast.makeText(this, "Sign-in failed or cancelled", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RC_RECOVER_AUTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Permission granted — tap Back Up Now / Restore again", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNav_view);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
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
        bottomNavigationView.getMenu().findItem(R.id.nav_settings).setChecked(true);
        updateNotificationBadge();
    }
}
