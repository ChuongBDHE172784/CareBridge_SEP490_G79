package com.carebridge.backend.map.routeprovider;

import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.dto.response.RouteResponse;
import com.carebridge.backend.map.dto.response.RoutePoint;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class RouteProvider {

    public RouteResponse getRoute(RouteRequest request) {
        RouteResponse response = new RouteResponse();
        response.setDistanceMeters(new BigDecimal("1200"));
        response.setEtaMinutes(15);
        response.setTransportMode(
                request.getTransportMode() != null ? request.getTransportMode() : "DRIVING"
        );

        List<RoutePoint> points = new ArrayList<>();
        points.add(new RoutePoint(10.0186, 105.7878, "Start"));
        points.add(new RoutePoint(10.0156, 105.7867, "Destination"));
        response.setPoints(points);

        return response;
    }

    public BigDecimal calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadius * c);
    }

    public int calculateEta(BigDecimal distanceMeters, String transportMode) {
        int speedKmh;
        if ("WALKING".equalsIgnoreCase(transportMode)) {
            speedKmh = 5;
        } else if ("MOTORCYCLE".equalsIgnoreCase(transportMode)) {
            speedKmh = 30;
        } else {
            speedKmh = 20;
        }
        double distanceKm = distanceMeters.doubleValue() / 1000;
        return Math.max(1, (int) Math.ceil(distanceKm / speedKmh * 60));
    }
}
