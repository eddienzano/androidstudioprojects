package com.hillary.transporter.map;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class TransporterMapManager implements OnMapReadyCallback {

    private final Context context;
    private final View container;

    private MapView mapView;
    private GoogleMap googleMap;

    private Marker driverMarker;
    private Marker destinationMarker;

    private Polyline routeLine;

    private LatLng driverPosition;
    private LatLng destinationPosition;

    private final RouteService routeService =
            new RouteService();

    public TransporterMapManager(
            Context context,
            View container) {

        this.context = context;
        this.container = container;
    }

    public void initMap() {

        mapView = (MapView) container;

        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {

        googleMap = map;

        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        googleMap.getUiSettings()
                .setMyLocationButtonEnabled(true);
    }

    public void updateDriverLocation(
            double lat,
            double lng) {

        if (googleMap == null)
            return;

        driverPosition =
                new LatLng(lat, lng);

        if (driverMarker == null) {

            driverMarker =
                    googleMap.addMarker(
                            new MarkerOptions()
                                    .position(driverPosition)
                                    .title("You")
                                    .icon(
                                            BitmapDescriptorFactory
                                                    .defaultMarker(
                                                            BitmapDescriptorFactory.HUE_AZURE
                                                    )
                                    )
                    );

        } else {

            driverMarker.setPosition(
                    driverPosition
            );
        }

        redrawRoute();
    }

    public void showRoute(
            String lat,
            String lng) {

        if (googleMap == null)
            return;

        try {

            double dLat =
                    Double.parseDouble(lat);

            double dLng =
                    Double.parseDouble(lng);

            destinationPosition =
                    new LatLng(
                            dLat,
                            dLng
                    );

            if (destinationMarker != null) {
                destinationMarker.remove();
            }

            destinationMarker =
                    googleMap.addMarker(
                            new MarkerOptions()
                                    .position(
                                            destinationPosition
                                    )
                                    .title(
                                            "Delivery Destination"
                                    )
                    );

            redrawRoute();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void redrawRoute() {

        if (googleMap == null)
            return;

        if (driverPosition == null)
            return;

        if (destinationPosition == null) {

            googleMap.animateCamera(
                    CameraUpdateFactory
                            .newLatLngZoom(
                                    driverPosition,
                                    16f
                            )
            );

            return;
        }

        routeService.fetchRoute(
                driverPosition.latitude,
                driverPosition.longitude,
                destinationPosition.latitude,
                destinationPosition.longitude,
                new RouteService.RouteCallback() {

                    @Override
                    public void onRouteLoaded(
                            RouteResult route) {

                        new Handler(
                                Looper.getMainLooper()
                        ).post(() -> {

                            try {

                                if (routeLine != null) {
                                    routeLine.remove();
                                }

                                List<LatLng> points =
                                        PolylineDecoder.decode(
                                                route.getPolyline()
                                        );

                                routeLine =
                                        googleMap.addPolyline(
                                                new PolylineOptions()
                                                        .addAll(points)
                                                        .width(12f)
                                                        .color(
                                                                0xFF1E88E5
                                                        )
                                                        .jointType(
                                                                JointType.ROUND
                                                        )
                                        );

                                LatLngBounds bounds =
                                        new LatLngBounds.Builder()
                                                .include(
                                                        driverPosition
                                                )
                                                .include(
                                                        destinationPosition
                                                )
                                                .build();

                                googleMap.animateCamera(
                                        CameraUpdateFactory
                                                .newLatLngBounds(
                                                        bounds,
                                                        150
                                                )
                                );

                            } catch (Exception e) {

                                e.printStackTrace();
                            }
                        });
                    }

                    @Override
                    public void onError(
                            Exception e) {

                        e.printStackTrace();
                    }
                }
        );
    }

    public void clearRoute() {

        if (routeLine != null) {

            routeLine.remove();
            routeLine = null;
        }

        if (destinationMarker != null) {

            destinationMarker.remove();
            destinationMarker = null;
        }

        destinationPosition = null;
    }

    public MapView getMapView() {
        return mapView;
    }
}