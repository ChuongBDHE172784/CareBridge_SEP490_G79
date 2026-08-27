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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    // Typing a name walks through its own prefixes, and several experts share the same
    // few hospitals, so the same upstream query repeats constantly. Ten minutes is long
    // enough to make that free and short enough that a new clinic still shows up.
    private static final long HOSPITAL_CACHE_TTL_MS = 10 * 60 * 1000L;
    private final Map<String, CacheEntry> hospitalCache = new ConcurrentHashMap<>();

    public TrackAsiaService() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static final Set<String> HEALTHCARE_TYPES = Set.of(
            "hospital", "health_and_wellness", "clinic", "doctor", "medical_lab");

    /** True when the provider tags the place as somewhere healthcare is delivered. */
    private static boolean isHealthcarePlace(Object types) {
        if (!(types instanceof List<?> list)) {
            return false;
        }
        for (Object type : list) {
            if (type instanceof String name && HEALTHCARE_TYPES.contains(name)) {
                return true;
            }
        }
        return false;
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
        // Text Search matches any place name, so a bare "Nhi" came back with a sex-toy
        // shop, a snack stall and a cafe. type=hospital is accepted and then ignored by
        // the API - it still answers with airports and preschools - so the words carry
        // the intent instead, and the types on each result do the actual filtering below.
        String named = typed.toLowerCase(Locale.ROOT).contains("bệnh viện")
                || typed.toLowerCase(Locale.ROOT).contains("phòng khám")
                ? typed
                : ("bệnh viện " + typed).trim();
        String effectiveQuery = province.isEmpty() ? named : named + " " + province;

        CacheEntry cached = hospitalCache.get(effectiveQuery);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < HOSPITAL_CACHE_TTL_MS) {
            @SuppressWarnings("unchecked")
            List<TrackAsiaPlaceDto> hit = (List<TrackAsiaPlaceDto>) cached.data();
            return hit;
        }

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
                List<TrackAsiaPlaceDto> medical = new ArrayList<>();
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
                        if (isHealthcarePlace(place.get("types"))) {
                            medical.add(dto);
                        }
                        results.add(dto);
                    }
                }
                // Prefer the places the provider actually labels as healthcare. A street
                // named after a hospital comes back as a "route", and a house number in
                // front of one as a "street_address"; neither is somewhere you work.
                // If nothing carries a health label, show the raw matches rather than an
                // empty box - a thin list is still something the expert can judge.
                List<TrackAsiaPlaceDto> answer = medical.isEmpty() ? results : medical;
                hospitalCache.put(effectiveQuery, new CacheEntry(answer, System.currentTimeMillis()));
                return answer;
            } else {
                log.error("TrackAsia API error: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Exception calling TrackAsia", e);
        }
        return List.of();
    }
}
