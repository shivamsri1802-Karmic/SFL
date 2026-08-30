package com.shivam.sfl;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/* JADX INFO: loaded from: classes2.dex */
public class MapsFragment extends AppCompatActivity implements OnMapReadyCallback {
    private double lat = 0.0d;
    private double longt = 0.0d;
    private GoogleMap mMap;

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.lat = getIntent().getDoubleExtra("lat", 0.0d);
        this.longt = getIntent().getDoubleExtra("long", 0.0d);
    }

    @Override // com.google.android.gms.maps.OnMapReadyCallback
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        LatLng currentLocation = new LatLng(this.lat, this.longt);
        this.mMap.setMinZoomPreference(15.0f);
        this.mMap.setMaxZoomPreference(25.0f);
        this.mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
        this.mMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.mMap.getUiSettings().setMapToolbarEnabled(false);
        this.mMap.getUiSettings().setZoomControlsEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
    }
}
