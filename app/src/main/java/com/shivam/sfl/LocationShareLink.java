package com.shivam.sfl;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds and parses sfl://location deep links for sharing individual locations via QR codes or links.
 */
public class LocationShareLink {
    private static final String SCHEME = "sfl";
    private static final String HOST = "location";

    public static String buildLink(SavedLocationEntity loc) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", loc.getName());
            o.put("lat", loc.getLat());
            o.put("lng", loc.getLongt());
            o.put("address", loc.getAddress());
            o.put("type", loc.getType());
            if (loc.getPlusCode() != null) {
                o.put("plusCode", loc.getPlusCode());
            }
            String json = o.toString();
            String encoded = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return SCHEME + "://" + HOST + "?data=" + encoded;
        } catch (JSONException e) {
            return null;
        }
    }

    public static SavedLocationEntity parseLink(Uri uri) {
        try {
            if (uri == null || !SCHEME.equals(uri.getScheme()) || !HOST.equals(uri.getHost())) return null;
            String encoded = uri.getQueryParameter("data");
            if (encoded == null) return null;
            byte[] bytes = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject o = new JSONObject(new String(bytes, StandardCharsets.UTF_8));

            SavedLocationEntity loc = new SavedLocationEntity();
            loc.setName(o.optString("name", "Shared Location"));
            loc.setLat(o.optDouble("lat", 0.0));
            loc.setLongt(o.optDouble("lng", 0.0));
            loc.setAddress(o.optString("address", ""));
            loc.setType(o.optString("type", "Other"));
            if (o.has("plusCode")) {
                loc.setPlusCode(o.optString("plusCode"));
            }
            loc.setTimeStamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            return loc;
        } catch (Exception e) {
            return null;
        }
    }
}
