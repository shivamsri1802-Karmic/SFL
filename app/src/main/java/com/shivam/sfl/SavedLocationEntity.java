package com.shivam.sfl;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes2.dex */
public class SavedLocationEntity {
    private int ID;
    private String address;
    private double lat;
    private double longt;
    private String name;
    private String plusCode;
    private String timeStamp;
    private String type;
    private float distanceMeters = -1f;
    private String contactId;
    private String contactName;

    public int getID() {
        return this.ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return this.lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLongt() {
        return this.longt;
    }

    public void setLongt(double longt) {
        this.longt = longt;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPlusCode() {
        return plusCode;
    }

    public void setPlusCode(String plusCode) {
        this.plusCode = plusCode;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public float getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(float distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String toString() {
        return "SavedLocationEntity{ID=" + this.ID + ", name='" + this.name + "', lat=" + this.lat + ", longt=" + this.longt + ", address='" + this.address + "', plusCode='" + this.plusCode + "', type='" + this.type + "', timeStamp=" + this.timeStamp + ", contactName='" + this.contactName + "'}";
    }

    public static SavedLocationEntity fromJson(JSONObject jsonObject) {
        SavedLocationEntity b = new SavedLocationEntity();
        try {
            b.ID = jsonObject.optInt("ID", -1);
            b.name = jsonObject.getString("name");
            b.lat = jsonObject.getDouble("lat");
            b.longt = jsonObject.getDouble("longt");
            b.address = jsonObject.getString("address");
            b.plusCode = jsonObject.optString("plusCode", null);
            b.type = jsonObject.getString("type");
            b.timeStamp = jsonObject.getString("timeStamp");
            b.contactId = jsonObject.optString("contactId", null);
            b.contactName = jsonObject.optString("contactName", null);
            return b;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
