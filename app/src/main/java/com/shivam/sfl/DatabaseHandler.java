package com.shivam.sfl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sfl_location";
    private static final int DATABASE_VERSION = 4;
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_DATE = "timestamp";
    private static final String KEY_ID = "id";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LONG = "longitude";
    private static final String KEY_NAME = "name";
    private static final String KEY_PLUS_CODE = "plus_code";
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONTACT_ID = "contact_id";
    private static final String KEY_CONTACT_NAME = "contact_name";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_COLLECTIONS = "collections";
    private static final String TABLE_LOCATION_COLLECTIONS = "location_collections";
    private final String TAG;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, DATABASE_VERSION);
        this.TAG = "SFL_Database_Handler";
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE locations(id INTEGER PRIMARY KEY,name TEXT,latitude TEXT,longitude TEXT,address TEXT,plus_code TEXT,type TEXT,timestamp TEXT,contact_id TEXT,contact_name TEXT);");
        createCollectionTables(db);
    }

    private void createCollectionTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS collections(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE);");
        db.execSQL("CREATE TABLE IF NOT EXISTS location_collections(location_id INTEGER,collection_id INTEGER,PRIMARY KEY(location_id, collection_id));");
    }

    public void addLocation(SavedLocationEntity savedLocationEntity) {
        Log.d("SFL_Database_Handler", "Inserting data to database");
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM locations WHERE id = ?", new String[]{String.valueOf(savedLocationEntity.getID())});
        if (c.moveToFirst()) {
            db.update(TABLE_LOCATIONS, toContentValues(savedLocationEntity), "id=" + savedLocationEntity.getID(), null);
            c.close();
            db.close();
            return;
        }
        c.close();
        db.insert(TABLE_LOCATIONS, null, toContentValues(savedLocationEntity));
        db.close();
    }

    public void addLocation(SavedLocationEntity savedLocationEntity, char u) {
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LOCATIONS, null, toContentValues(savedLocationEntity));
        db.close();
    }

    public long insertLocation(SavedLocationEntity savedLocationEntity) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(TABLE_LOCATIONS, null, toContentValues(savedLocationEntity));
        db.close();
        return id;
    }

    private ContentValues toContentValues(SavedLocationEntity savedLocationEntity) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, savedLocationEntity.getName());
        values.put(KEY_LAT, Double.valueOf(savedLocationEntity.getLat()));
        values.put(KEY_LONG, Double.valueOf(savedLocationEntity.getLongt()));
        values.put(KEY_ADDRESS, savedLocationEntity.getAddress());
        values.put(KEY_PLUS_CODE, savedLocationEntity.getPlusCode());
        values.put(KEY_TYPE, savedLocationEntity.getType());
        values.put(KEY_DATE, savedLocationEntity.getTimeStamp());
        values.put(KEY_CONTACT_ID, savedLocationEntity.getContactId());
        values.put(KEY_CONTACT_NAME, savedLocationEntity.getContactName());
        return values;
    }

    private SavedLocationEntity mapCursorToEntity(Cursor cursor) {
        SavedLocationEntity location = new SavedLocationEntity();
        location.setID(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        location.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
        location.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LAT)));
        location.setLongt(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONG)));
        location.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ADDRESS)));
        location.setPlusCode(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLUS_CODE)));
        location.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
        location.setTimeStamp(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
        location.setContactId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_ID)));
        location.setContactName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NAME)));
        return location;
    }

    public List<SavedLocationEntity> getAllLocations() {
        List<SavedLocationEntity> locationList = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT  * FROM locations", null);
        Log.d("SFL_Database_Handler", String.valueOf(cursor.getCount()));
        if (cursor.moveToFirst()) {
            do {
                locationList.add(mapCursorToEntity(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return locationList;
    }

    public SavedLocationEntity getLocationById(int id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM locations WHERE id = ?", new String[]{String.valueOf(id)});
        SavedLocationEntity location = null;
        if (cursor.moveToFirst()) {
            location = mapCursorToEntity(cursor);
        }
        cursor.close();
        db.close();
        return location;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE locations ADD COLUMN plus_code TEXT;");
        }
        if (oldVersion < 3) {
            createCollectionTables(db);
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE locations ADD COLUMN contact_id TEXT;");
            db.execSQL("ALTER TABLE locations ADD COLUMN contact_name TEXT;");
        }
    }

    public void deleteLocation(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LOCATIONS, "id=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_LOCATION_COLLECTIONS, "location_id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public long getOrCreateCollection(String name) {
        SQLiteDatabase db = getWritableDatabase();
        String trimmedName = name.trim();
        Cursor c = db.rawQuery("SELECT id FROM collections WHERE name = ? COLLATE NOCASE", new String[]{trimmedName});
        if (c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            db.close();
            return id;
        }
        c.close();
        ContentValues values = new ContentValues();
        values.put("name", trimmedName);
        long id = db.insert(TABLE_COLLECTIONS, null, values);
        db.close();
        return id;
    }

    public List<CollectionEntity> getAllCollections() {
        List<CollectionEntity> collections = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.name, (SELECT COUNT(*) FROM location_collections lc WHERE lc.collection_id = c.id) "
                        + "FROM collections c ORDER BY c.name COLLATE NOCASE", null);
        if (cursor.moveToFirst()) {
            do {
                CollectionEntity entity = new CollectionEntity();
                entity.setId(cursor.getInt(0));
                entity.setName(cursor.getString(1));
                entity.setLocationCount(cursor.getInt(2));
                collections.add(entity);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return collections;
    }

    public void addLocationToCollection(int locationId, int collectionId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("location_id", locationId);
        values.put("collection_id", collectionId);
        db.insertWithOnConflict(TABLE_LOCATION_COLLECTIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void deleteCollection(int collectionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_COLLECTIONS, "id=?", new String[]{String.valueOf(collectionId)});
        db.delete(TABLE_LOCATION_COLLECTIONS, "collection_id=?", new String[]{String.valueOf(collectionId)});
        db.close();
    }

    public List<SavedLocationEntity> getLocationsInCollection(int collectionId) {
        List<SavedLocationEntity> locationList = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT l.* FROM locations l INNER JOIN location_collections lc ON l.id = lc.location_id WHERE lc.collection_id = ?",
                new String[]{String.valueOf(collectionId)});
        if (cursor.moveToFirst()) {
            do {
                locationList.add(mapCursorToEntity(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return locationList;
    }
}
