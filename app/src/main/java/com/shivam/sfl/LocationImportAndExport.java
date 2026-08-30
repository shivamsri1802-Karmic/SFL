package com.shivam.sfl;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public class LocationImportAndExport {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String TAG = "SFL_ImportData";
    private static LocationManager locationManager;
    private static DatabaseHandler mDatabaseHandler;

    public static void importLocation(int requestCode, int code, Intent data, Context mContext) {
        mDatabaseHandler = new DatabaseHandler(mContext);
        if (data == null) {
            Toast.makeText(mContext.getApplicationContext(), "No file selected to import", 0).show();
            return;
        }
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(data.getData());
            StringBuffer fileContent = new StringBuffer("");
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                while (inputStream.available() > 0) {
                    if (inputStream.available() < 1024) {
                        buffer = new byte[inputStream.available()];
                    }
                    inputStream.read(buffer);
                    fileContent.append(new String(buffer, 0, buffer.length));
                }
                String a = new String(fileContent);
                Log.d(TAG, a);
                JSONArray jsonArray = new JSONArray(a);
                ArrayList<SavedLocationEntity> locations = new ArrayList<>(jsonArray.length());
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject locationJson = jsonArray.getJSONObject(i);
                        SavedLocationEntity location = SavedLocationEntity.fromJson(locationJson);
                        if (location != null) {
                            locations.add(location);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                inputStream.close();
                for (SavedLocationEntity sle : locations) {
                    mDatabaseHandler.addLocation(sle, 'u');
                }
                Intent mIntent = new Intent(mContext, (Class<?>) SavedLocationList.class);
                mIntent.addFlags(268435456);
                mContext.startActivity(new Intent(mContext, (Class<?>) SavedLocationList.class));
            }
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        } catch (JSONException e4) {
            Toast.makeText(mContext.getApplicationContext(), "Please selected SFL exported file", 0).show();
            e4.printStackTrace();
        }
    }

    public static void exportLocation(Context mContext) {
        mDatabaseHandler = new DatabaseHandler(mContext);
        exportLocations(mContext, mDatabaseHandler.getAllLocations());
    }

    public static JSONArray toJsonArray(List<SavedLocationEntity> list) throws JSONException {
        JSONArray jsArray = new JSONArray();
        for (SavedLocationEntity l : list) {
            JSONObject savedLocationEntity = new JSONObject();
            savedLocationEntity.put("ID", l.getID());
            savedLocationEntity.put("name", l.getName());
            savedLocationEntity.put("lat", l.getLat());
            savedLocationEntity.put("longt", l.getLongt());
            savedLocationEntity.put("address", l.getAddress());
            savedLocationEntity.put("type", l.getType());
            savedLocationEntity.put("timeStamp", l.getTimeStamp());
            if (l.getContactId() != null) savedLocationEntity.put("contactId", l.getContactId());
            if (l.getContactName() != null) savedLocationEntity.put("contactName", l.getContactName());
            jsArray.put(savedLocationEntity);
        }
        return jsArray;
    }

    public static void exportLocations(Context mContext, List<SavedLocationEntity> list) {
        JSONArray jsArray = new JSONArray();
        try {
            jsArray = toJsonArray(list);
            try {
                File path = mContext.getFilesDir();
                File file = new File(path, "favorite_location.json");
                Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName(), file);
                FileOutputStream outputStreamWriter = new FileOutputStream(file);
                outputStreamWriter.write(jsArray.toString().getBytes());
                outputStreamWriter.close();
                mContext.grantUriPermission(mContext.getPackageName(), contentUri, 3);
                Intent sharingIntent = new Intent("android.intent.action.SEND");
                sharingIntent.setType(URLConnection.guessContentTypeFromName(file.getName()));
                sharingIntent.setData(contentUri);
                sharingIntent.putExtra("android.intent.extra.STREAM", contentUri);
                sharingIntent.addFlags(1);
                sharingIntent.addFlags(2);
                sharingIntent.addFlags(268435456);
                mContext.startActivity(Intent.createChooser(sharingIntent, "Share Your Favorite Location"));
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        } catch (JSONException e2) {
            Log.e("Exception", "JSON Export failed: " + e2.toString());
            Toast.makeText(mContext, "Export Failed", 1);
        }
        Log.d(TAG, jsArray.toString());
    }

    public static boolean checkGpsStatus(Context mContext) {
        LocationManager locationManager2 = (LocationManager) mContext.getSystemService("location");
        locationManager = locationManager2;
        boolean gpsStatus = locationManager2.isProviderEnabled("gps");
        if (gpsStatus) {
            return true;
        }
        return false;
    }
}
