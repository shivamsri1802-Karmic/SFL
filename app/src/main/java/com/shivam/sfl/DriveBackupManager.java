package com.shivam.sfl;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Opt-in backup/restore of the location list to Google Drive's hidden "app data" folder
 * (invisible in the user's normal Drive UI, readable only by this app). This is a snapshot
 * backup/restore, not continuous two-way sync - matching the roadmap's "low-lift first
 * step" framing. No Drive REST client library is used; requests are plain HttpURLConnection
 * calls to keep the dependency footprint small.
 */
public class DriveBackupManager {
    private static final String SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata";
    private static final String AUTH_SCOPE = "oauth2:" + SCOPE_APPDATA;
    private static final String BACKUP_FILE_NAME = "sfl_backup.json";
    private static final String KEY_LAST_SYNCED = "last_synced";
    private static final String KEY_SYNC_ENABLED = "cloud_sync_enabled";

    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
        void onRecoverableError(Intent recoveryIntent);
    }

    public static GoogleSignInClient getSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(SCOPE_APPDATA))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    public static GoogleSignInAccount getSignedInAccount(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    public static boolean isSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SYNC_ENABLED, false);
    }

    public static void setSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
    }

    public static String getLastSyncedText(Context context) {
        return prefs(context).getString(KEY_LAST_SYNCED, null);
    }

    private static void setLastSynced(Context context, String text) {
        prefs(context).edit().putString(KEY_LAST_SYNCED, text).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(SflApplication.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void backupNow(Context context, DatabaseHandler db, Callback callback) {
        AppExecutors.getInstance().runOnBackground(() -> {
            try {
                GoogleSignInAccount account = getSignedInAccount(context);
                if (account == null) {
                    postError(callback, "Not signed in to Google Drive");
                    return;
                }
                String token = fetchToken(context, account);
                int count = db.getAllLocations().size();
                String content = LocationImportAndExport.toJsonArray(db.getAllLocations()).toString();

                String existingFileId = findBackupFileId(token);
                if (existingFileId != null) {
                    updateFile(token, existingFileId, content);
                } else {
                    createFile(token, content);
                }

                String timestamp = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(new Date());
                setLastSynced(context, timestamp);
                postSuccess(callback, "Backed up " + count + " location(s)");
            } catch (UserRecoverableAuthException e) {
                postRecoverable(callback, e.getIntent());
            } catch (Exception e) {
                postError(callback, "Backup failed: " + e.getMessage());
            }
        });
    }

    public static void restoreNow(Context context, DatabaseHandler db, Callback callback) {
        AppExecutors.getInstance().runOnBackground(() -> {
            try {
                GoogleSignInAccount account = getSignedInAccount(context);
                if (account == null) {
                    postError(callback, "Not signed in to Google Drive");
                    return;
                }
                String token = fetchToken(context, account);
                String fileId = findBackupFileId(token);
                if (fileId == null) {
                    postError(callback, "No backup found in Google Drive yet");
                    return;
                }
                JSONArray arr = new JSONArray(downloadFile(token, fileId));
                int imported = 0;
                for (int i = 0; i < arr.length(); i++) {
                    SavedLocationEntity entity = SavedLocationEntity.fromJson(arr.getJSONObject(i));
                    if (entity != null) {
                        db.addLocation(entity, 'u');
                        imported++;
                    }
                }
                postSuccess(callback, "Restored " + imported + " location(s) from Drive");
            } catch (UserRecoverableAuthException e) {
                postRecoverable(callback, e.getIntent());
            } catch (Exception e) {
                postError(callback, "Restore failed: " + e.getMessage());
            }
        });
    }

    private static String fetchToken(Context context, GoogleSignInAccount account) throws IOException, GoogleAuthException {
        Account androidAccount = account.getAccount();
        if (androidAccount == null) throw new IOException("No account available on this device");
        return GoogleAuthUtil.getToken(context, androidAccount, AUTH_SCOPE);
    }

    private static String findBackupFileId(String token) throws IOException, JSONException {
        String query = URLEncoder.encode("name='" + BACKUP_FILE_NAME + "'", "UTF-8");
        String fields = URLEncoder.encode("files(id)", "UTF-8");
        String urlStr = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=" + query + "&fields=" + fields;
        HttpURLConnection conn = openGet(urlStr, token);
        JSONObject json = new JSONObject(readResponse(conn));
        JSONArray files = json.optJSONArray("files");
        return (files != null && files.length() > 0) ? files.getJSONObject(0).getString("id") : null;
    }

    private static void createFile(String token, String content) throws IOException, JSONException {
        String boundary = "sfl_backup_boundary";
        JSONObject metadata = new JSONObject();
        metadata.put("name", BACKUP_FILE_NAME);
        metadata.put("parents", new JSONArray().put("appDataFolder"));

        String body = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/json\r\n\r\n"
                + content + "\r\n"
                + "--" + boundary + "--";

        URL url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        writeBody(conn, body);
        readResponse(conn);
    }

    private static void updateFile(String token, String fileId, String content) throws IOException {
        URL url = new URL("https://www.googleapis.com/upload/drive/v3/files/" + fileId + "?uploadType=media");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PATCH");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        writeBody(conn, content);
        readResponse(conn);
    }

    private static String downloadFile(String token, String fileId) throws IOException {
        return readResponse(openGet("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media", token));
    }

    private static HttpURLConnection openGet(String urlStr, String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        if (code < 200 || code >= 300) throw new IOException("Drive API error " + code + ": " + sb);
        return sb.toString();
    }

    private static void postSuccess(Callback callback, String message) {
        AppExecutors.getInstance().runOnMainThread(() -> callback.onSuccess(message));
    }

    private static void postError(Callback callback, String message) {
        AppExecutors.getInstance().runOnMainThread(() -> callback.onError(message));
    }

    private static void postRecoverable(Callback callback, Intent intent) {
        AppExecutors.getInstance().runOnMainThread(() -> callback.onRecoverableError(intent));
    }
}
