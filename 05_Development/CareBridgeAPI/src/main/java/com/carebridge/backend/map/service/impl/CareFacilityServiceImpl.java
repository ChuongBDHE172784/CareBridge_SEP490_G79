package com.carebridge.backend.map.service.impl;

import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.dto.response.NearbyResponse;
import com.carebridge.backend.map.dto.response.RoutePoint;
import com.carebridge.backend.map.dto.response.RouteResponse;
import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.exception.MapException;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.map.trackasia.TrackAsiaClient;
import com.carebridge.backend.map.service.ICareFacilityService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareFacilityServiceImpl implements ICareFacilityService {

    private final TrackAsiaClient trackAsiaClient;
    private final CareFacilityRepository facilityRepository;

    @Override
    public List<FacilityResponse> getAllFacilities() {
        return List.of();
    }

    @Override
    public FacilityResponse getFacilityById(UUID id) {
        CareFacility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new MapException(HttpStatus.NOT_FOUND, "MAP-002", "Facility not found"));
        return toResponse(facility);
    }

    @Override
    public NearbyResponse searchNearby(BigDecimal lat, BigDecimal lng, Integer radiusMeters, String type) {
        int radius = radiusMeters != null ? radiusMeters : 5000;

        // Try TrackAsia first
        try {
            JsonNode root = trackAsiaClient.searchNearby(lat.doubleValue(), lng.doubleValue(), radius, type);
            if (root != null) {
                List<FacilityResponse> results = parseTrackAsiaResults(root);
                if (!results.isEmpty()) {
                    return new NearbyResponse(results, results.size());
                }
            }
        } catch (Exception e) {
            log.error("[CareFacility] TrackAsia search failed: {}", e.getMessage());
        }

        // Fallback: DB-verified facilities
        List<CareFacility> dbResults = facilityRepository.findNearby(lat, lng, radius);
        List<FacilityResponse> verified = new ArrayList<>();
        for (CareFacility f : dbResults) {
            verified.add(toResponse(f));
        }
        return new NearbyResponse(verified, verified.size());
    }

    private List<FacilityResponse> parseTrackAsiaResults(JsonNode root) {
        List<FacilityResponse> results = new ArrayList<>();
        JsonNode features = root.get("features");
        if (features == null || !features.isArray()) {
            return results;
        }

        for (JsonNode feature : features) {
            JsonNode poi = feature.get("poi");
            JsonNode props = poi != null && poi.isObject() ? poi.get("properties") : null;
            if (props == null) continue;

            String name = safeText(props, "name");
            String address = safeText(props, "address");
            String phone = safeText(props, "tel");
            String category = safeText(props, "category");

            BigDecimal fLat = null, fLng = null;
            JsonNode geom = feature.get("geometry");
            if (geom != null && geom.isObject()) {
                JsonNode coords = geom.get("coordinates");
                if (coords != null && coords.isArray() && coords.size() >= 2) {
                    fLng = BigDecimal.valueOf(coords.get(0).asDouble());
                    fLat = BigDecimal.valueOf(coords.get(1).asDouble());
                }
            }

            int distanceM = 0;
            JsonNode distMeta = feature.get("properties");
            if (distMeta != null && distMeta.has("distance")) {
                distanceM = distMeta.get("distance").asInt();
            }

            results.add(FacilityResponse.builder()
                    .name(name)
                    .address(address)
                    .phone(phone)
                    .facilityType(category)
                    .latitude(fLat)
                    .longitude(fLng)
                    .sourceType("TRACKASIA")
                    .verificationStatus("UNVERIFIED")
                    .distanceMeters(distanceM > 0 ? distanceM : null)
                    .build());
        }
        return results;
    }

    @Override
    public RouteResponse getRoute(RouteRequest request) {
        try {
            JsonNode root = trackAsiaClient.route(
                    request.getFromLat().doubleValue(),
                    request.getFromLng().doubleValue(),
                    request.getToLat().doubleValue(),
                    request.getToLng().doubleValue(),
                    request.getTransportMode());

            JsonNode routes = root.get("routes");
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                throw new MapException(HttpStatus.NOT_FOUND, "MAP-003", "No route found");
            }

            JsonNode leg = routes.get(0);
            double distanceM = leg.path("distance").asDouble();
            int durationSec = leg.path("duration").asInt();
            int etaMin = Math.max(1, (int) Math.ceil(durationSec / 60.0));

            List<RoutePoint> points = new ArrayList<>();
            JsonNode steps = leg.path("steps");
            if (steps.isArray()) {
                for (JsonNode step : steps) {
                    JsonNode mani = step.path("maneuver");
                    if (mani.isObject() && mani.has("location")) {
                        JsonNode loc = mani.get("location");
                        if (loc.isArray() && loc.size() >= 2) {
                            points.add(new RoutePoint(
                                    BigDecimal.valueOf(loc.get(1).asDouble()).doubleValue(),
                                    BigDecimal.valueOf(loc.get(0).asDouble()).doubleValue(),
                                    safeText(mani, "type")));
                        }
                    }
                }
            }

            return new RouteResponse(
                    BigDecimal.valueOf(Math.round(distanceM)),
                    etaMin,
                    points,
                    request.getTransportMode() != null ? request.getTransportMode() : "DRIVING");
        } catch (MapException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CareFacility] TrackAsia route failed: {}", e.getMessage());
            throw new MapException(HttpStatus.INTERNAL_SERVER_ERROR, "MAP-004",
                    "Failed to get route: " + e.getMessage());
        }
    }

    private FacilityResponse toResponse(CareFacility f) {
        return FacilityResponse.builder()
                .facilityId(f.getFacilityId())
                .partnerId(f.getPartnerId())
                .name(f.getName())
                .facilityType(f.getFacilityType())
                .address(f.getAddress())
                .latitude(f.getLatitude())
                .longitude(f.getLongitude())
                .phone(f.getPhone())
                .openingHoursJson(f.getOpeningHoursJson())
                .sourceType(f.getSourceType() != null ? f.getSourceType() : "MANUAL")
                .verificationStatus(f.getVerificationStatus() != null ? f.getVerificationStatus().name() : "UNVERIFIED")
                .build();
    }

    private static String safeText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null && !child.isNull() ? child.asText() : null;
    }
}
