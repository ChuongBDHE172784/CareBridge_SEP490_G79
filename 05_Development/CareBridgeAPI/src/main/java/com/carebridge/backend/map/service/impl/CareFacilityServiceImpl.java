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

    private static final int MAX_NEARBY_RADIUS_METERS = 50_000;

    private final TrackAsiaClient trackAsiaClient;
    private final CareFacilityRepository facilityRepository;

    @Override
    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FacilityResponse getFacilityById(UUID id) {
        CareFacility facility = facilityRepository.findByFacilityIdAndActiveTrue(id)
                .orElseThrow(() -> new MapException(HttpStatus.NOT_FOUND, "MAP-002", "Facility not found"));
        return toResponse(facility);
    }

    @Override
    public List<FacilityResponse> getPendingFacilities() {
        return facilityRepository.findByVerificationStatus(com.carebridge.backend.map.facilitystatus.FacilityStatus.UNVERIFIED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void verifyFacility(UUID facilityId, com.carebridge.backend.map.facilitystatus.FacilityStatus status, UUID adminId) {
        CareFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new MapException(HttpStatus.NOT_FOUND, "MAP-002", "Facility not found"));
        facility.setVerificationStatus(status);
        facilityRepository.save(facility);
    }

    @Override
    public NearbyResponse searchNearby(BigDecimal lat, BigDecimal lng, Integer radiusMeters, String type,
                                       String provinceId, String districtId) {
        int radius = radiusMeters != null ? radiusMeters : 5000;
        validateNearbyInput(lat, lng, radius);
        String normalizedType = normalizeFilter(type);

        // External results do not carry canonical geography IDs, so they cannot
        // safely satisfy province/district filters.
        if (provinceId == null && districtId == null) {
            try {
                JsonNode root = trackAsiaClient.searchNearby(lat.doubleValue(), lng.doubleValue(), radius, normalizedType);
                if (root != null) {
                    List<FacilityResponse> results = parseTrackAsiaResults(root);
                    if (!results.isEmpty()) {
                        return new NearbyResponse(results, results.size());
                    }
                }
            } catch (Exception e) {
                log.error("[CareFacility] TrackAsia search failed: {}", e.getMessage());
            }
        }

        // Fallback: DB-verified facilities
        List<CareFacility> dbResults = facilityRepository.findNearby(
                lat, lng, radius, normalizedType, provinceId, districtId);
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
            try {
                JsonNode poi = feature.get("poi");
                JsonNode props = poi != null && poi.isObject() ? poi.get("properties") : null;
                if (props == null || !props.isObject()) continue;

                JsonNode geom = feature.get("geometry");
                JsonNode coords = geom != null && geom.isObject() ? geom.get("coordinates") : null;
                if (coords == null || !coords.isArray() || coords.size() < 2
                        || !coords.get(0).isNumber() || !coords.get(1).isNumber()) {
                    continue;
                }
                double longitude = coords.get(0).doubleValue();
                double latitude = coords.get(1).doubleValue();
                if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                        || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    continue;
                }
                BigDecimal fLat = BigDecimal.valueOf(latitude);
                BigDecimal fLng = BigDecimal.valueOf(longitude);

                int distanceM = 0;
                JsonNode distMeta = feature.get("properties");
                if (distMeta != null && distMeta.has("distance")) {
                    distanceM = Math.max(0, distMeta.get("distance").asInt());
                }

                results.add(FacilityResponse.builder()
                        .name(safeText(props, "name"))
                        .address(safeText(props, "address"))
                        .phone(safeText(props, "tel"))
                        .facilityType(safeText(props, "category"))
                        .latitude(fLat)
                        .longitude(fLng)
                        .sourceType("TRACKASIA")
                        .externalSourceId(safeText(feature, "id"))
                        .verificationStatus("UNVERIFIED")
                        .active(true)
                        .searchable(true)
                        .distanceMeters(distanceM > 0 ? distanceM : null)
                        .build());
            } catch (RuntimeException malformedFeature) {
                log.warn("[CareFacility] Ignoring malformed TrackAsia feature");
            }
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
                .facilityLevel(f.getFacilityLevel())
                .ownershipType(f.getOwnershipType())
                .address(f.getAddress())
                .provinceId(f.getProvinceId())
                .districtId(f.getDistrictId())
                .latitude(f.getLatitude())
                .longitude(f.getLongitude())
                .phone(f.getPhone())
                .openingHoursJson(f.getOpeningHoursJson())
                .sourceType(f.getSourceType() != null ? f.getSourceType() : "MANUAL")
                .externalSourceId(f.getExternalSourceId())
                .verificationStatus(f.getVerificationStatus() != null ? f.getVerificationStatus().name() : "UNVERIFIED")
                .active(f.getActive())
                .searchable(f.getSearchable())
                .build();
    }

    private static String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateNearbyInput(BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
        if (latitude == null || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new MapException(HttpStatus.BAD_REQUEST, "MAP-005", "Latitude must be between -90 and 90");
        }
        if (longitude == null || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new MapException(HttpStatus.BAD_REQUEST, "MAP-006", "Longitude must be between -180 and 180");
        }
        if (radiusMeters <= 0 || radiusMeters > MAX_NEARBY_RADIUS_METERS) {
            throw new MapException(HttpStatus.BAD_REQUEST, "MAP-007",
                    "Radius must be between 1 and " + MAX_NEARBY_RADIUS_METERS + " meters");
        }
    }

    private static String safeText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null && !child.isNull() ? child.asText() : null;
    }
}
