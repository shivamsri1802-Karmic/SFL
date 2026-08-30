package com.shivam.sfl;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds and parses sfl://collection deep links. There's no backend for this app, so a
 * "shareable link" can only be a self-contained URI that another copy of SFL decodes
 * locally - it isn't a universal web link anyone can open, only one an installed copy
 * of this app can resolve.
 */
public class CollectionShareLink {
    private static final String SCHEME = "sfl";
    private static final String HOST = "collection";

    public static class SharedCollection {
        public String name;
        public final List<SavedLocationEntity> locations = new ArrayList<>();
    }

    public static String buildLink(String collectionName, List<SavedLocationEntity> locations) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("collectionName", collectionName);
            JSONArray arr = new JSONArray();
            for (SavedLocationEntity loc : locations) {
                JSONObject o = new JSONObject();
                o.put("name", loc.getName());
                o.put("lat", loc.getLat());
                o.put("lng", loc.getLongt());
                o.put("address", loc.getAddress());
                o.put("type", loc.getType());
                arr.put(o);
            }
            payload.put("locations", arr);
            String json = payload.toString();
            String encoded = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return SCHEME + "://" + HOST + "?data=" + encoded;
        } catch (JSONException e) {
            return null;
        }
    }

    public static SharedCollection parseLink(Uri uri) {
        try {
            if (uri == null || !SCHEME.equals(uri.getScheme()) || !HOST.equals(uri.getHost())) return null;
            String encoded = uri.getQueryParameter("data");
            if (encoded == null) return null;
            byte[] bytes = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject payload = new JSONObject(new String(bytes, StandardCharsets.UTF_8));

            SharedCollection result = new SharedCollection();
            result.name = payload.optString("collectionName", "Shared Collection");
            JSONArray arr = payload.optJSONArray("locations");
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    SavedLocationEntity loc = new SavedLocationEntity();
                    loc.setName(o.optString("name"));
                    loc.setLat(o.optDouble("lat"));
                    loc.setLongt(o.optDouble("lng"));
                    loc.setAddress(o.optString("address", ""));
                    loc.setType(o.optString("type", ""));
                    loc.setTimeStamp(now);
                    result.locations.add(loc);
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
