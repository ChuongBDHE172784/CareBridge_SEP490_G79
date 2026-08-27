package com.carebridge.backend.map;

import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.dto.request.RouteRequest;
import com.carebridge.backend.map.exception.MapException;
import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.map.service.impl.CareFacilityServiceImpl;
import com.carebridge.backend.map.trackasia.TrackAsiaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareFacilityServiceImplTest {

    @Mock private TrackAsiaClient trackAsiaClient;
    @Mock private CareFacilityRepository facilityRepository;
    @InjectMocks private CareFacilityServiceImpl service;

    @Test
    void getAllFacilitiesReturnsActiveCanonicalFacilities() {
        CareFacility facility = facility();
        when(facilityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(facility));

        var result = service.getAllFacilities();

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.getFacilityId()).isEqualTo(facility.getFacilityId());
            assertThat(response.getSourceType()).isEqualTo("MANUAL");
            assertThat(response.getActive()).isTrue();
        });
    }

    @Test
    void facilityDetailUsesCanonicalActiveIdentity() {
        CareFacility facility = facility();
        when(facilityRepository.findByFacilityIdAndActiveTrue(facility.getFacilityId()))
                .thenReturn(Optional.of(facility));

        var result = service.getFacilityById(facility.getFacilityId());

        assertThat(result.getFacilityId()).isEqualTo(facility.getFacilityId());
    }

    @Test
    void trackAsiaFailureNeverFallsBackToCareFacilities() throws Exception {
        when(trackAsiaClient.searchNearby(10.0, 105.0, 5000, "hospital"))
                .thenThrow(new RuntimeException("provider unavailable"));

        assertThatThrownBy(() -> service.searchNearby(
                new BigDecimal("10.0"), new BigDecimal("105.0"), 5000, "hospital"))
                .isInstanceOf(MapException.class)
                .hasMessageContaining("TrackAsia is temporarily unavailable");
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void trackAsiaV2ResultIsNormalizedAndDistanceSorted() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.readTree("""
                {"status":"OK","results":[
                  {"place_id":"far","name":"Hospital Far","types":["hospital"],
                   "geometry":{"location":{"lat":10.010,"lng":105.010}}},
                  {"place_id":"near","name":"Hospital Near","formatted_address":"Can Tho",
                   "types":["hospital"],"geometry":{"location":{"lat":10.001,"lng":105.001}}}
                ]}
                """);
        when(trackAsiaClient.searchNearby(10.0, 105.0, 5000, "hospital")).thenReturn(payload);

        var result = service.searchNearby(BigDecimal.TEN, new BigDecimal("105"), 5000, null);

        assertThat(result.getFacilities()).extracting("name")
                .containsExactly("Hospital Near", "Hospital Far");
        assertThat(result.getFacilities().getFirst().getFacilityId()).isNull();
        assertThat(result.getFacilities().getFirst().getSourceType()).isEqualTo("TRACKASIA");
        assertThat(result.getFacilities().getFirst().getVerificationStatus()).isEqualTo("UNVERIFIED");
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void zeroResultsIsSuccessfulAndDoesNotUseDatabase() throws Exception {
        var payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"status\":\"ZERO_RESULTS\",\"results\":[]}");
        when(trackAsiaClient.searchNearby(10.0, 105.0, 5000, "hospital")).thenReturn(payload);

        var result = service.searchNearby(BigDecimal.TEN, new BigDecimal("105"), 5000, null);

        assertThat(result.getFacilities()).isEmpty();
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void providerErrorStatusIsNotReportedAsZeroResults() throws Exception {
        var payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"status\":\"INVALID_REQUEST\",\"error_message\":\"bad request\"}");
        when(trackAsiaClient.searchNearby(10.0, 105.0, 5000, "hospital")).thenReturn(payload);

        assertThatThrownBy(() -> service.searchNearby(
                BigDecimal.TEN, new BigDecimal("105"), 5000, "hospital"))
                .isInstanceOf(MapException.class)
                .hasMessageContaining("TrackAsia is temporarily unavailable");
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void malformedTrackAsiaPlaceIsSkippedWithoutDroppingValidResults() throws Exception {
        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {"status":"OK","results":[
                  {"name":"Bad","geometry":{"location":{"lat":10,"lng":999}}},
                  {"place_id":"good","name":"Good","types":["hospital"],
                   "geometry":{"location":{"lat":10.001,"lng":105.001}}}
                ]}
                """);
        when(trackAsiaClient.searchNearby(10.0, 105.0, 5000, "hospital")).thenReturn(payload);

        var result = service.searchNearby(
                new BigDecimal("10"), new BigDecimal("105"), 5000, null);

        assertThat(result.getFacilities()).singleElement()
                .satisfies(response -> assertThat(response.getName()).isEqualTo("Good"));
    }

    @Test
    void routeUsesTrackAsiaPolylineDurationAndSteps() throws Exception {
        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {"code":"Ok","routes":[{"geometry":"encoded-polyline6","distance":1250.4,
                  "duration":301,"legs":[{"steps":[{"distance":200.2,"duration":45,
                    "name":"Nguyen Trai","maneuver":{"type":"turn","modifier":"right",
                    "location":[105.001,10.001]}}]}]}]}
                """);
        when(trackAsiaClient.route(10.0, 105.0, 10.01, 105.01, "MOTORCYCLE"))
                .thenReturn(payload);
        var request = RouteRequest.builder()
                .fromLat(BigDecimal.TEN).fromLng(new BigDecimal("105"))
                .toLat(new BigDecimal("10.01")).toLng(new BigDecimal("105.01"))
                .transportMode("MOTORCYCLE").build();

        var result = service.getRoute(request);

        assertThat(result.getDistanceMeters()).isEqualByComparingTo("1250");
        assertThat(result.getDurationSeconds()).isEqualTo(301);
        assertThat(result.getEtaMinutes()).isEqualTo(6);
        assertThat(result.getEncodedPolyline()).isEqualTo("encoded-polyline6");
        assertThat(result.getSteps()).singleElement().satisfies(step -> {
            assertThat(step.getManeuver()).isEqualTo("turn-right");
            assertThat(step.getRoadName()).isEqualTo("Nguyen Trai");
        });
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void routeWithoutTrackAsiaGeometryIsRejected() throws Exception {
        var payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                {"code":"Ok","routes":[{"distance":1250.4,"duration":301,"legs":[]}]}
                """);
        when(trackAsiaClient.route(10.0, 105.0, 10.01, 105.01, "DRIVING"))
                .thenReturn(payload);
        var request = RouteRequest.builder()
                .fromLat(BigDecimal.TEN).fromLng(new BigDecimal("105"))
                .toLat(new BigDecimal("10.01")).toLng(new BigDecimal("105.01"))
                .transportMode("DRIVING").build();

        assertThatThrownBy(() -> service.getRoute(request))
                .isInstanceOf(MapException.class)
                .hasMessageContaining("invalid route");
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void invalidCoordinatesRadiusAndTypeAreRejectedBeforeExternalCalls() {
        assertInvalid(new BigDecimal("90.0001"), BigDecimal.ZERO, 5000, null);
        assertInvalid(BigDecimal.ZERO, new BigDecimal("180.0001"), 5000, null);
        assertInvalid(BigDecimal.ZERO, BigDecimal.ZERO, 1000, null);
        assertInvalid(BigDecimal.ZERO, BigDecimal.ZERO, 50_000, null);
        assertInvalid(BigDecimal.ZERO, BigDecimal.ZERO, 5000, "pharmacy");
        verifyNoInteractions(trackAsiaClient, facilityRepository);
    }

    private void assertInvalid(BigDecimal latitude, BigDecimal longitude, int radius, String type) {
        assertThatThrownBy(() -> service.searchNearby(latitude, longitude, radius, type))
                .isInstanceOf(MapException.class);
    }

    private static CareFacility facility() {
        return CareFacility.builder()
                .facilityId(UUID.randomUUID())
                .name("Canonical hospital")
                .facilityType("HOSPITAL")
                .sourceType("MANUAL")
                .active(true)
                .searchable(true)
                .verificationStatus(FacilityStatus.VERIFIED)
                .build();
    }
}
