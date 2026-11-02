package com.hillary.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hillary.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity
        extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    private TextView tvAddress;
    private TextView tvCoordinates;

    private double selectedLat = 0;
    private double selectedLng = 0;

    private String selectedAddress = "";

    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map_picker);

        /* =========================
           INIT VIEWS
        ========================= */

        tvAddress =
                findViewById(R.id.tvAddress);

        tvCoordinates =
                findViewById(R.id.tvCoordinates);

        MaterialButton btnConfirm =
                findViewById(R.id.btnConfirmLocation);

        FloatingActionButton btnMyLocation =
                findViewById(R.id.btnMyLocation);

        /* =========================
           LOCATION CLIENT
        ========================= */

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        /* =========================
           MAP FRAGMENT
        ========================= */

        SupportMapFragment mapFragment =
                (SupportMapFragment)
                        getSupportFragmentManager()
                                .findFragmentById(R.id.map);

        if(mapFragment != null){

            mapFragment.getMapAsync(this);
        }

        /* =========================
           CONFIRM LOCATION
        ========================= */

        btnConfirm.setOnClickListener(v -> {

            if(selectedLat == 0 || selectedLng == 0){

                Toast.makeText(
                        this,
                        "Please select location",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent data = new Intent();

            data.putExtra(
                    "latitude",
                    selectedLat
            );

            data.putExtra(
                    "longitude",
                    selectedLng
            );

            data.putExtra(
                    "address",
                    selectedAddress
            );

            setResult(
                    RESULT_OK,
                    data
            );

            finish();
        });

        /* =========================
           MY LOCATION BUTTON
        ========================= */

        btnMyLocation.setOnClickListener(v -> {

            getCurrentLocation();
        });
    }

    @Override
    public void onMapReady(
            @NonNull GoogleMap googleMap
    ) {

        mMap = googleMap;

        /* =========================
           MAP SETTINGS
        ========================= */

        mMap.getUiSettings()
                .setZoomControlsEnabled(false);

        mMap.getUiSettings()
                .setCompassEnabled(true);

        mMap.getUiSettings()
                .setMyLocationButtonEnabled(false);

        mMap.getUiSettings()
                .setRotateGesturesEnabled(true);

        mMap.getUiSettings()
                .setTiltGesturesEnabled(true);

        mMap.setMapType(
                GoogleMap.MAP_TYPE_NORMAL
        );

        /* =========================
           DEFAULT LOCATION
        ========================= */

        LatLng nairobi =
                new LatLng(
                        -1.286389,
                        36.817223
                );

        mMap.moveCamera(
                CameraUpdateFactory
                        .newLatLngZoom(
                                nairobi,
                                15f
                        )
        );

        /* =========================
           CAMERA LISTENER
        ========================= */

        mMap.setOnCameraIdleListener(() -> {

            LatLng center =
                    mMap.getCameraPosition()
                            .target;

            selectedLat =
                    center.latitude;

            selectedLng =
                    center.longitude;

            tvCoordinates.setText(
                    "Lat: "
                            + selectedLat
                            + " | Lng: "
                            + selectedLng
            );

            getReadableAddress(
                    selectedLat,
                    selectedLng
            );
        });

        /* =========================
           AUTO LOAD CURRENT LOCATION
        ========================= */

        getCurrentLocation();
    }

    /* =====================================
       GET CURRENT LOCATION
    ===================================== */

    private void getCurrentLocation(){

        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_REQUEST_CODE
            );

            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                )
                .addOnSuccessListener(location -> {

                    if(location != null){

                        LatLng latLng =
                                new LatLng(
                                        location.getLatitude(),
                                        location.getLongitude()
                                );

                        mMap.animateCamera(
                                CameraUpdateFactory
                                        .newLatLngZoom(
                                                latLng,
                                                17f
                                        )
                        );
                    }
                });
    }

    /* =====================================
       REVERSE GEOCODING
    ===================================== */

    private void getReadableAddress(
            double lat,
            double lng
    ){

        Geocoder geocoder =
                new Geocoder(
                        this,
                        Locale.getDefault()
                );

        try {

            List<Address> addresses =
                    geocoder.getFromLocation(
                            lat,
                            lng,
                            1
                    );

            if(addresses != null
                    && !addresses.isEmpty()){

                Address address =
                        addresses.get(0);

                selectedAddress =
                        address.getAddressLine(0);

                tvAddress.setText(
                        selectedAddress
                );

            } else {

                tvAddress.setText(
                        "Unknown location"
                );
            }

        } catch (IOException e){

            e.printStackTrace();

            tvAddress.setText(
                    "Unable to fetch address"
            );
        }
    }

    /* =====================================
       PERMISSION RESULT
    ===================================== */

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if(requestCode == LOCATION_REQUEST_CODE){

            if(grantResults.length > 0
                    &&
                    grantResults[0]
                            == PackageManager.PERMISSION_GRANTED){

                getCurrentLocation();

            } else {

                Toast.makeText(
                        this,
                        "Location permission denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}