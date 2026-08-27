package com.carebridge.backend.map.service.impl;

import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.dto.response.FacilityResponse;
import com.carebridge.backend.map.dto.response.NearbyResponse;
import com.carebridge.backend.map.dto.response.RoutePoint;
import com.carebridge.backend.map.dto.response.RouteResponse;
import com.carebridge.backend.map.dto.response.RouteStep;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareFacilityServiceImpl implements ICareFacilityService {

    private static final Set<Integer> SUPPORTED_RADII_METERS = Set.of(5_000, 10_000, 15_000);

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
    public NearbyResponse searchNearby(BigDecimal lat, BigDecimal lng, Integer radiusMeters, String type) {
        int radius = radiusMeters != null ? radiusMeters : 5000;
        validateNearbyInput(lat, lng, radius);
        String normalizedType = normalizeFacilityType(type);
        try {
            JsonNode root = trackAsiaClient.searchNearby(
                    lat.doubleValue(), lng.doubleValue(), radius, normalizedType);
            List<FacilityResponse> results = parseTrackAsiaResults(
                    root, lat.doubleValue(), lng.doubleValue(), radius, normalizedType);
            return new NearbyResponse(results, results.size());
        } catch (Exception e) {
            log.warn("[CareFacility] TrackAsia nearby request failed ({})",
                    e.getClass().getSimpleName());
            throw new MapException(HttpStatus.BAD_GATEWAY, "MAP-008",
                    "TrackAsia is temporarily unavailable");
        }
    }

    private List<FacilityResponse> parseTrackAsiaResults(
            JsonNode root, double originLat, double originLng, int radiusMeters,
            String requestedType) {
        List<FacilityResponse> results = new ArrayList<>();
        if (root == null) {
            throw new IllegalArgumentException("Missing TrackAsia nearby payload");
        }
        String status = root.path("status").asText("");
        if ("ZERO_RESULTS".equalsIgnoreCase(status)) {
            return results;
        }
        if (!"OK".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid TrackAsia nearby status");
        }
        JsonNode places = root.get("results");
        if (places == null || !places.isArray()) {
            throw new IllegalArgumentException("Missing TrackAsia nearby results");
        }

        for (JsonNode place : places) {
            try {
                String name = safeText(place, "name");
                JsonNode location = place.path("geometry").path("location");
                String facilityType = resolveFacilityType(place.path("types"), requestedType);
                double latitude = location.path("lat").asDouble(Double.NaN);
                double longitude = location.path("lng").asDouble(Double.NaN);
                if (name == null || name.isBlank() || facilityType == null) continue;
                if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                        || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    continue;
                }
                int distanceM = haversineMeters(originLat, originLng, latitude, longitude);
                // TrackAsia documents that dense-area results may exceed the requested radius.
                if (distanceM > radiusMeters) continue;

                results.add(FacilityResponse.builder()
                        .name(name)
                        .address(safeText(place, "formatted_address"))
                        .facilityType(facilityType)
                        .latitude(BigDecimal.valueOf(latitude))
                        .longitude(BigDecimal.valueOf(longitude))
                        .sourceType("TRACKASIA")
                        .externalSourceId(safeText(place, "place_id"))
                        .verificationStatus("UNVERIFIED")
                        .active(true)
                        .searchable(true)
                        .distanceMeters(distanceM)
                        .build());
            } catch (RuntimeException malformedFeature) {
                log.debug("[CareFacility] Ignoring malformed TrackAsia place");
            }
        }
        return results.stream()
                .sorted(Comparator.comparing(FacilityResponse::getDistanceMeters))
                .limit(20)
                .toList();
    }

    @Override
    public RouteResponse getRoute(RouteRequest request) {
        String transportMode = normalizeTransportMode(request.getTransportMode());
        try {
            JsonNode root = trackAsiaClient.route(
                    request.getFromLat().doubleValue(),
                    request.getFromLng().doubleValue(),
                    request.getToLat().doubleValue(),
                    request.getToLng().doubleValue(),
                    transportMode);

            JsonNode routes = root.get("routes");
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                throw new MapException(HttpStatus.NOT_FOUND, "MAP-003", "No route found");
            }

            JsonNode route = routes.get(0);
            String providerCode = root.path("code").asText("Ok");
            JsonNode distanceNode = route.get("distance");
            JsonNode durationNode = route.get("duration");
            String encodedPolyline = route.path("geometry").asText(null);
            if (!"Ok".equalsIgnoreCase(providerCode)
                    || distanceNode == null || !distanceNode.isNumber()
                    || durationNode == null || !durationNode.isNumber()
                    || encodedPolyline == null || encodedPolyline.isBlank()) {
                throw new MapException(HttpStatus.BAD_GATEWAY, "MAP-004",
                        "TrackAsia returned an invalid route");
            }
            double distanceM = distanceNode.asDouble();
            int durationSec = durationNode.asInt();
            if (!Double.isFinite(distanceM) || distanceM < 0 || durationSec < 0) {
                throw new MapException(HttpStatus.BAD_GATEWAY, "MAP-004",
                        "TrackAsia returned an invalid route");
            }
            int etaMin = Math.max(1, (int) Math.ceil(durationSec / 60.0));

            List<RoutePoint> points = new ArrayList<>();
            List<RouteStep> routeSteps = new ArrayList<>();
            JsonNode legs = route.path("legs");
            if (legs.isArray()) {
                for (JsonNode leg : legs) {
                    JsonNode steps = leg.path("steps");
                    if (!steps.isArray()) continue;
                    for (JsonNode step : steps) {
                        JsonNode mani = step.path("maneuver");
                        JsonNode loc = mani.path("location");
                        if (!loc.isArray() || loc.size() < 2
                                || !loc.get(0).isNumber() || !loc.get(1).isNumber()) continue;
                        double stepLat = loc.get(1).asDouble();
                        double stepLng = loc.get(0).asDouble();
                        if (!validCoordinate(stepLat, stepLng)) continue;
                        String maneuver = maneuverText(mani);
                        points.add(new RoutePoint(stepLat, stepLng, maneuver));
                        routeSteps.add(RouteStep.builder()
                                .lat(stepLat)
                                .lng(stepLng)
                                .maneuver(maneuver)
                                .roadName(safeText(step, "name"))
                                .distanceMeters(Math.max(0,
                                        (int) Math.round(step.path("distance").asDouble())))
                                .durationSeconds(Math.max(0, step.path("duration").asInt()))
                                .build());
                        }
                    }
                }

            return RouteResponse.builder()
                    .distanceMeters(BigDecimal.valueOf(Math.round(distanceM)))
                    .etaMinutes(etaMin)
                    .durationSeconds(durationSec)
                    .encodedPolyline(encodedPolyline)
                    .points(points)
                    .steps(routeSteps)
                    .transportMode(transportMode)
                    .build();
        } catch (MapException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[CareFacility] TrackAsia route request failed ({})",
                    e.getClass().getSimpleName());
            throw new MapException(HttpStatus.BAD_GATEWAY, "MAP-004",
                    "TrackAsia route is temporarily unavailable");
        }
    }

    private FacilityResponse toResponse(CareFacility f) {
        return FacilityResponse.builder()
                .facilityId(f.getFacilityId())
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

    private static String normalizeFacilityType(String value) {
        String normalized = value == null || value.isBlank()
                ? "hospital" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("hospital", "clinic", "health_station").contains(normalized)) {
            throw new MapException(HttpStatus.BAD_REQUEST, "MAP-009", "Unsupported facility type");
        }
        return normalized;
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
        if (!SUPPORTED_RADII_METERS.contains(radiusMeters)) {
            throw new MapException(HttpStatus.BAD_REQUEST, "MAP-007",
                    "Radius must be one of 5000, 10000, or 15000 meters");
        }
    }

    private static String normalizeTransportMode(String value) {
        String mode = value == null || value.isBlank() ? "DRIVING" : value.trim().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "DRIVING", "CAR" -> "DRIVING";
            case "MOTORCYCLE", "MOTORCYCLING", "MOTO" -> "MOTORCYCLE";
            case "WALKING", "WALK" -> "WALKING";
            default -> throw new MapException(HttpStatus.BAD_REQUEST, "MAP-010", "Unsupported transport mode");
        };
    }

    private static String resolveFacilityType(JsonNode types, String requestedType) {
        if (types.isArray()) {
            for (JsonNode type : types) {
                String value = type.asText("").toLowerCase(Locale.ROOT);
                if (requestedType.equals(value)) return value.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static boolean validCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    private static String maneuverText(JsonNode maneuver) {
        String type = maneuver.path("type").asText("continue");
        String modifier = maneuver.path("modifier").asText("");
        return modifier.isBlank() ? type : type + "-" + modifier;
    }

    private static int haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000d;
        double latDelta = Math.toRadians(lat2 - lat1);
        double lngDelta = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);
        return (int) Math.round(earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private static String safeText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null && !child.isNull() ? child.asText() : null;
    }
}
