package com.hillary.transporter.map;

import android.util.Log;

import com.hillary.network.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RouteService {

    public interface RouteCallback {
        void onRouteLoaded(RouteResult route);
        void onError(Exception e);
    }

    private final OkHttpClient client =
            new OkHttpClient();

    public void fetchRoute(
            double originLat,
            double originLng,
            double destLat,
            double destLng,
            RouteCallback callback) {

        String url =
                Constants.GOOGLE_DIRECTIONS_URL
                        + "?origin=" + originLat + "," + originLng
                        + "&destination=" + destLat + "," + destLng
                        + "&mode=driving"
                        + "&key=" + Constants.GOOGLE_MAPS_API_KEY;

        Log.d("ROUTE_URL", url);

        Request request =
                new Request.Builder()
                        .url(url)
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(
                    Call call,
                    IOException e) {

                Log.e(
                        "ROUTE_ERROR",
                        "Request failed",
                        e
                );

                callback.onError(e);
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response)
                    throws IOException {

                try {

                    String body =
                            response.body().string();

                    Log.d(
                            "DIRECTIONS_RESPONSE",
                            body
                    );

                    JSONObject json =
                            new JSONObject(body);

                    String status =
                            json.optString("status");

                    Log.d(
                            "DIRECTIONS_STATUS",
                            status
                    );

                    if (!"OK".equals(status)) {

                        callback.onError(
                                new Exception(
                                        "Directions Status: "
                                                + status
                                )
                        );

                        return;
                    }

                    JSONArray routes =
                            json.getJSONArray("routes");

                    if (routes.length() == 0) {

                        callback.onError(
                                new Exception(
                                        "No routes returned"
                                )
                        );

                        return;
                    }

                    JSONObject route =
                            routes.getJSONObject(0);

                    JSONObject leg =
                            route.getJSONArray("legs")
                                    .getJSONObject(0);

                    String distance =
                            leg.getJSONObject("distance")
                                    .getString("text");

                    String duration =
                            leg.getJSONObject("duration")
                                    .getString("text");

                    String polyline =
                            route.getJSONObject(
                                            "overview_polyline")
                                    .getString("points");

                    Log.d(
                            "ROUTE_DISTANCE",
                            distance
                    );

                    Log.d(
                            "ROUTE_DURATION",
                            duration
                    );

                    Log.d(
                            "ROUTE_POLYLINE",
                            polyline
                    );

                    callback.onRouteLoaded(
                            new RouteResult(
                                    distance,
                                    duration,
                                    polyline
                            )
                    );

                } catch (Exception e) {

                    Log.e(
                            "ROUTE_PARSE_ERROR",
                            "Failed parsing route",
                            e
                    );

                    callback.onError(e);
                }
            }
        });
    }
}