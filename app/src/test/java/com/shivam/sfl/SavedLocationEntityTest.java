package com.shivam.sfl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Plain JUnit tests for SavedLocationEntity's JSON (de)serialization, which is exactly what
 * LocationImportAndExport relies on for backup/restore. These don't touch the Android
 * framework, so they run as fast, local unit tests (no emulator/device needed).
 */
public class SavedLocationEntityTest {

    @Test
    public void fromJson_parsesAllFieldsCorrectly() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("ID", 7);
        json.put("name", "Home");
        json.put("lat", 12.34);
        json.put("longt", 56.78);
        json.put("address", "123 Main St");
        json.put("type", "Home Goods Store");
        json.put("timeStamp", "2026-08-29 10:15:00");

        SavedLocationEntity entity = SavedLocationEntity.fromJson(json);

        assertEquals(7, entity.getID());
        assertEquals("Home", entity.getName());
        assertEquals(12.34, entity.getLat(), 0.0001);
        assertEquals(56.78, entity.getLongt(), 0.0001);
        assertEquals("123 Main St", entity.getAddress());
        assertEquals("Home Goods Store", entity.getType());
        assertEquals("2026-08-29 10:15:00", entity.getTimeStamp());
    }

    @Test
    public void fromJson_returnsNullWhenARequiredFieldIsMissing() throws JSONException {
        // Missing "lat" - this is the shape of a corrupt/foreign import file.
        JSONObject json = new JSONObject();
        json.put("ID", 1);
        json.put("name", "Incomplete");
        json.put("longt", 1.0);
        json.put("address", "nowhere");
        json.put("type", "Park");
        json.put("timeStamp", "2026-08-29 10:15:00");

        assertNull(SavedLocationEntity.fromJson(json));
    }
}
