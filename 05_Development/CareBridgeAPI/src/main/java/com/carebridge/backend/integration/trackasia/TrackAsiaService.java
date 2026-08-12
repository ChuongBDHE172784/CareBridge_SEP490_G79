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

    // maps.trackasia.vn does not exist. It resolved only because the ISP answers
    // NXDOMAIN with a landing page, so every hospital lookup failed at connect time
    // and the catch below turned that into an empty result list - the box simply
    // never suggested anything. The host that serves the API, and that the emergency
    // map has been using all along, is maps.track-asia.com.
    private static final String BASE_URL = "https://maps.track-asia.com/api/v2";

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

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    public List<TrackAsiaPlaceDto> searchHospitals(String query) {
        return searchHospitals(query, null);
    }

    /**
     * Finds hospitals by name, optionally narrowed to one province.
     *
     * <p>TrackAsia has no province filter and no paging: Text Search returns at most 20
     * results and Nearby Search at most 10, and neither honours page, limit or a page
     * token - measured against the live API. Naming the province inside the query text is
     * what actually moves the results into that province, so an expert in Đà Nẵng is
     * offered hospitals in Đà Nẵng rather than the same national list every time.
     */
    public List<TrackAsiaPlaceDto> searchHospitals(String query, String provinceName) {
        String typed = query == null ? "" : query.trim();
        String province = provinceName == null ? "" : provinceName.trim();
        if (typed.isEmpty() && province.isEmpty()) {
            return List.of();
        }
        // With no name typed yet, browse the province: "bệnh viện <tỉnh>" is what turns
        // a province choice into a usable list to pick from.
        String effectiveQuery = typed.isEmpty()
                ? "bệnh viện " + province
                : (province.isEmpty() ? typed : typed + " " + province);

        try {
            // Place Text Search: the endpoint that answers "find me the hospital called
            // X". The old call named a path (/search/autocomplete), a key parameter
            // (api_key) and a query parameter (text) that this API does not have; with
            // the right host it answers 404, and with the wrong one it never connects.
            String encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8);
            String url = BASE_URL + "/place/textsearch/json?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&query=" + encodedQuery
                    + "&new_admin=true";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                List<TrackAsiaPlaceDto> results = new ArrayList<>();
                // Place Text Search answers with a flat "results" array, not the GeoJSON
                // "features" the old code walked, and the coordinates arrive as a named
                // lat/lng pair rather than a [lng, lat] tuple.
                if (body.get("results") instanceof List<?> places) {
                    for (Object placeObj : places) {
                        if (!(placeObj instanceof Map<?, ?> place)) {
                            continue;
                        }
                        Object name = place.get("name");
                        if (name == null) {
                            continue;
                        }
                        TrackAsiaPlaceDto dto = new TrackAsiaPlaceDto();
                        dto.setPlaceId(asString(place.get("place_id")));
                        dto.setName(asString(name));
                        dto.setLabel(asString(place.get("formatted_address")));
                        if (place.get("geometry") instanceof Map<?, ?> geometry
                                && geometry.get("location") instanceof Map<?, ?> location) {
                            if (location.get("lat") instanceof Number lat) {
                                dto.setLatitude(lat.doubleValue());
                            }
                            if (location.get("lng") instanceof Number lng) {
                                dto.setLongitude(lng.doubleValue());
                            }
                        }
                        results.add(dto);
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
