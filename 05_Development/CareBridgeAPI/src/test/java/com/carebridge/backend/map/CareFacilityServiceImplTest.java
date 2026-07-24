package com.carebridge.backend.map;

import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.facilitystatus.FacilityStatus;
import com.carebridge.backend.map.exception.MapException;
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

    @Mock
    private TrackAsiaClient trackAsiaClient;

    @Mock
    private CareFacilityRepository facilityRepository;

    @InjectMocks
    private CareFacilityServiceImpl service;

    @Test
    void getAllFacilitiesReturnsActiveCanonicalFacilities() {
        CareFacility facility = facility();
        when(facilityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(facility));

        var result = service.getAllFacilities();

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.getFacilityId()).isEqualTo(facility.getFacilityId());
            assertThat(response.getPartnerId()).isNull();
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
        verify(facilityRepository).findByFacilityIdAndActiveTrue(facility.getFacilityId());
    }

    @Test
    void geographyFiltersSkipExternalProviderAndUseMetersAndCanonicalFilters() throws Exception {
        when(facilityRepository.findNearby(
                new BigDecimal("10.0"), new BigDecimal("105.0"), 5000,
                "hospital", "92", "916"))
                .thenReturn(List.of(facility()));

        var result = service.searchNearby(
                new BigDecimal("10.0"), new BigDecimal("105.0"), 5000,
                " hospital ", "92", "916");

        assertThat(result.getFacilities()).hasSize(1);
        verify(facilityRepository).findNearby(
                new BigDecimal("10.0"), new BigDecimal("105.0"), 5000,
                "hospital", "92", "916");
        verifyNoInteractions(trackAsiaClient);
    }

    @Test
    void trackAsiaResultDoesNotRequireCanonicalOrPartnerId() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.readTree("""
                {"features":[{"poi":{"properties":{"name":"External clinic","category":"CLINIC"}},
                "geometry":{"coordinates":[105.7,10.1]},"properties":{"distance":120}}]}
                """);
        when(trackAsiaClient.searchNearby(anyDouble(), anyDouble(), anyInt(), isNull())).thenReturn(payload);

        var result = service.searchNearby(
                BigDecimal.TEN, new BigDecimal("105"), 1000, null, null, null);

        assertThat(result.getFacilities()).singleElement().satisfies(response -> {
            assertThat(response.getFacilityId()).isNull();
            assertThat(response.getPartnerId()).isNull();
            assertThat(response.getSourceType()).isEqualTo("TRACKASIA");
            assertThat(response.getVerificationStatus()).isEqualTo("UNVERIFIED");
        });
        verifyNoInteractions(facilityRepository);
    }

    @Test
    void invalidCoordinatesAndRadiusAreRejectedBeforeProviderOrDatabaseAccess() {
        assertInvalid(new BigDecimal("90.0001"), BigDecimal.ZERO, 1000);
        assertInvalid(new BigDecimal("-90.0001"), BigDecimal.ZERO, 1000);
        assertInvalid(BigDecimal.ZERO, new BigDecimal("180.0001"), 1000);
        assertInvalid(BigDecimal.ZERO, new BigDecimal("-180.0001"), 1000);
        assertInvalid(BigDecimal.ZERO, BigDecimal.ZERO, 0);
        assertInvalid(BigDecimal.ZERO, BigDecimal.ZERO, 50_001);

        verifyNoInteractions(trackAsiaClient, facilityRepository);
    }

    @Test
    void malformedTrackAsiaFeatureIsSkippedWithoutDroppingValidResults() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.readTree("""
                {"features":[
                  {"poi":{"properties":{"name":"Bad"}},"geometry":{"coordinates":[999,10]}},
                  {"poi":{"properties":{"name":"Good","category":"HOSPITAL"}},
                   "geometry":{"coordinates":[105.7,10.1]}}
                ]}
                """);
        when(trackAsiaClient.searchNearby(10.0, 105.0, 1000, null)).thenReturn(payload);

        var result = service.searchNearby(
                new BigDecimal("10"), new BigDecimal("105"), 1000, null, null, null);

        assertThat(result.getFacilities()).singleElement()
                .satisfies(response -> assertThat(response.getName()).isEqualTo("Good"));
    }

    private void assertInvalid(BigDecimal latitude, BigDecimal longitude, int radius) {
        assertThatThrownBy(() -> service.searchNearby(latitude, longitude, radius, null, null, null))
                .isInstanceOf(MapException.class);
    }

    private static CareFacility facility() {
        return CareFacility.builder()
                .facilityId(UUID.randomUUID())
                .name("Canonical hospital")
                .facilityType("HOSPITAL")
                .active(true)
                .searchable(true)
                .verificationStatus(FacilityStatus.VERIFIED)
                .build();
    }
}
