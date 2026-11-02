package com.hillary.transporter.map;

public class RouteResult {

    private final String distance;
    private final String duration;
    private final String polyline;

    public RouteResult(String distance,
                       String duration,
                       String polyline) {

        this.distance = distance;
        this.duration = duration;
        this.polyline = polyline;
    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }

    public String getPolyline() {
        return polyline;
    }
}