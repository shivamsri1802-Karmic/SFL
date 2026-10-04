package com.shivam.sfl;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeocoderAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private static final String TAG = "GeocoderAdapter";
    private final Geocoder geocoder;
    private final List<String> resultList = new ArrayList<>();

    public GeocoderAutoCompleteAdapter(Context context, Geocoder geocoder) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.geocoder = geocoder;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Nullable
    @Override
    public String getItem(int index) {
        if (index >= 0 && index < resultList.size()) {
            return resultList.get(index);
        }
        return null;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() >= 2) {
                    List<String> addresses = findAddresses(constraint.toString());
                    filterResults.values = addresses;
                    filterResults.count = addresses.size();
                } else {
                    filterResults.values = new ArrayList<String>();
                    filterResults.count = 0;
                }
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                resultList.clear();
                if (results != null && results.values != null) {
                    resultList.addAll((List<String>) results.values);
                }
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    private List<String> findAddresses(String query) {
        List<String> addressStrings = new ArrayList<>();
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 5);
            if (addresses != null) {
                for (Address address : addresses) {
                    StringBuilder sb = new StringBuilder();
                    if (address.getMaxAddressLineIndex() >= 0) {
                        sb.append(address.getAddressLine(0));
                    } else {
                        if (address.getFeatureName() != null) {
                            sb.append(address.getFeatureName()).append(", ");
                        }
                        if (address.getLocality() != null) {
                            sb.append(address.getLocality()).append(", ");
                        }
                        if (address.getAdminArea() != null) {
                            sb.append(address.getAdminArea()).append(", ");
                        }
                        if (address.getCountryName() != null) {
                            sb.append(address.getCountryName());
                        }
                    }
                    String result = sb.toString().trim();
                    if (!result.isEmpty() && !addressStrings.contains(result)) {
                        addressStrings.add(result);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Autocomplete geocoding failed", e);
        }
        return addressStrings;
    }
}
