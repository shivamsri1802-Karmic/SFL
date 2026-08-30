package com.shivam.sfl;

import android.content.Context;
import android.content.SharedPreferences;

public class QuickSetLocations {
    private static final String KEY_HOME_ID = "home_location_id";
    private static final String KEY_WORK_ID = "work_location_id";
    private static final int NONE = -1;

    public static int getHomeLocationId(Context context) {
        return prefs(context).getInt(KEY_HOME_ID, NONE);
    }

    public static int getWorkLocationId(Context context) {
        return prefs(context).getInt(KEY_WORK_ID, NONE);
    }

    public static void setHomeLocationId(Context context, int id) {
        prefs(context).edit().putInt(KEY_HOME_ID, id).apply();
    }

    public static void setWorkLocationId(Context context, int id) {
        prefs(context).edit().putInt(KEY_WORK_ID, id).apply();
    }

    public static void clearHomeIfMatches(Context context, int id) {
        if (getHomeLocationId(context) == id) prefs(context).edit().remove(KEY_HOME_ID).apply();
    }

    public static void clearWorkIfMatches(Context context, int id) {
        if (getWorkLocationId(context) == id) prefs(context).edit().remove(KEY_WORK_ID).apply();
    }

    public static void clearIfMatches(Context context, int id) {
        clearHomeIfMatches(context, id);
        clearWorkIfMatches(context, id);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(SflApplication.PREFS_NAME, Context.MODE_PRIVATE);
    }
}
