package com.carebridge.backend.integration.trackasia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TrackAsiaService {

    @Value("${TRACKASIA_API_KEY}")
    private String apiKey;

    private static final String BASE_URL = "https://maps.trackasia.vn/api/v2";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Optional short-lived cache for emergency map
    private final Map<String, CacheEntry> emergencyCache = new ConcurrentHashMap<>();

    private record CacheEntry(Object data, long timestamp) {}

    public TrackAsiaService() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<TrackAsiaPlaceDto> searchHospitals(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "/search/autocomplete?api_key=" + apiKey 
                    + "&text=" + encodedQuery 
                    + "&new_admin=true"; // the prompt asked for new_admin=true

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                List<TrackAsiaPlaceDto> results = new ArrayList<>();
                if (body.get("features") instanceof List features) {
                    for (Object fObj : features) {
                        if (fObj instanceof Map fMap) {
                            Map props = (Map) fMap.get("properties");
                            Map geom = (Map) fMap.get("geometry");
                            if (props != null && geom != null) {
                                // Filter for hospitals or medical facilities
                                String layer = (String) props.get("layer");
                                if ("venue".equals(layer) || "address".equals(layer)) {
                                    TrackAsiaPlaceDto dto = new TrackAsiaPlaceDto();
                                    dto.setPlaceId((String) props.get("id"));
                                    dto.setName((String) props.get("name"));
                                    dto.setLabel((String) props.get("label"));
                                    
                                    List<Number> coords = (List<Number>) geom.get("coordinates");
                                    if (coords != null && coords.size() >= 2) {
                                        dto.setLongitude(coords.get(0).doubleValue());
                                        dto.setLatitude(coords.get(1).doubleValue());
                                    }
                                    results.add(dto);
                                }
                            }
                        }
                    }
                }
                return results;
            } else {
                log.error("TrackAsia API error: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Exception calling TrackAsia", e);
        }
        return List.of();
    }
}
