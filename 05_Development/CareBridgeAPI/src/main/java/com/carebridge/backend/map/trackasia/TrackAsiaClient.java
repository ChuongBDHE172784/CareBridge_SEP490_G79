package com.carebridge.backend.map.trackasia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Slf4j
public class TrackAsiaClient {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String mapsBaseUrl;

    public TrackAsiaClient(
            @Value("${carebridge.trackasia.api-key:}") String apiKey,
            @Value("${carebridge.trackasia.maps-base-url:https://maps.track-asia.com}") String mapsBaseUrl) {
        this.apiKey = apiKey;
        this.mapsBaseUrl = mapsBaseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    private HttpRequest.Builder baseRequest() {
        return HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    public JsonNode searchNearby(double lat, double lng, int radiusMeters, String type)
            throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("TRACKASIA_API_KEY not configured");
        }

        String requestedType = type == null || type.isBlank() ? "hospital" : type.trim();
        String query = "location=" + encode(String.format(Locale.ROOT, "%.7f,%.7f", lat, lng))
                + "&radius=" + radiusMeters
                + "&type=" + encode(requestedType)
                + "&new_admin=true"
                + "&key=" + encode(apiKey);
        String fullUrl = mapsBaseUrl + "/api/v2/place/nearbysearch/json?" + query;

        var request = baseRequest()
                .uri(URI.create(fullUrl))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());
        log.debug("[TrackAsia] places status={}", response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "TrackAsia Places HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    public JsonNode route(double fromLat, double fromLng,
                          double toLat, double toLng, String transportMode)
            throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("TRACKASIA_API_KEY not configured");
        }

        String coordinates = String.format(Locale.ROOT,
                "%f,%f;%f,%f",
                fromLng, fromLat, toLng, toLat);
        String profile = switch (transportMode == null ? "DRIVING" : transportMode.trim().toUpperCase(Locale.ROOT)) {
            case "DRIVING", "CAR" -> "car";
            case "MOTORCYCLE", "MOTORCYCLING", "MOTO" -> "moto";
            case "WALKING", "WALK" -> "walk";
            default -> throw new IllegalArgumentException("Unsupported transport mode");
        };
        String url = mapsBaseUrl + "/route/v1/" + profile + "/" + coordinates
                + ".json?geometries=polyline6&steps=true&overview=full&alternatives=false&key="
                + encode(apiKey);

        var request = baseRequest()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());
        log.debug("[TrackAsia] directions status={}", response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "TrackAsia Directions HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    public JsonNode searchPlace(String query, double lat, double lng)
            throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("TRACKASIA_API_KEY not configured");
        }

        String q = "query=" + encode(query)
                + "&location=" + encode(String.format(Locale.ROOT, "%.7f,%.7f", lat, lng))
                + "&key=" + encode(apiKey);

        var request = baseRequest()
                .uri(URI.create(mapsBaseUrl + "/api/v2/place/textsearch/json?" + q))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());
        log.debug("[TrackAsia] search status={}", response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "TrackAsia Search HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
