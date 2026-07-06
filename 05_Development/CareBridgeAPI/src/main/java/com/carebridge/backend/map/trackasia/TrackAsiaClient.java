package com.carebridge.backend.map.trackasia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class TrackAsiaClient {

    private static final String PLACES_URL =
            "https://api.track-asia.com/v1/search/nearby/json";
    private static final String DIRECTIONS_URL =
            "https://api.track-asia.com/v1/directions/driving/";
    private static final String SEARCH_URL =
            "https://api.track-asia.com/v1/search/search/json";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public TrackAsiaClient(
            @Value("${carebridge.trackasia.api-key:}") String apiKey) {
        this.apiKey = apiKey;
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

        String query = String.format(
                "poi=true&lat=%f&lon=%f&radius=%d&limit=20",
                lat, lng, radiusMeters);

        if (type != null && !type.isBlank()) {
            query += "&type=" + type;
        }

        String fullUrl = PLACES_URL + "?" + query;

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

        String coordinates = String.format(
                "%f,%f;%f,%f",
                fromLng, fromLat, toLng, toLat);

        String url = DIRECTIONS_URL + coordinates
                + "?alternatives=false&overview=full";

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

        String q = String.format(
                "q=%s&lat=%f&lon=%f&limit=5",
                query.replace(" ", "+"), lat, lng);

        var request = baseRequest()
                .uri(URI.create(SEARCH_URL + "?" + q))
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
}
