package com.carebridge.backend.expertavailability.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpertAvailabilityMapperTest {

    private final ExpertAvailabilityMapper mapper = new ExpertAvailabilityMapper();

    @Test
    void newAvailabilityDefaultsToAvailableWhenRequestOmitsStatus() {
        var request = CreateAvailabilityRequest.builder()
                .startAt(Instant.parse("2026-08-01T11:00:00Z"))
                .endAt(Instant.parse("2026-08-01T13:30:00Z"))
                .channelType("ONLINE_CHAT")
                .build();

        var availability = mapper.toEntity(UUID.randomUUID(), request);

        assertThat(availability.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    // The create request carries an optional initial status so an expert can publish a slot that
    // is already taken; a recognised value is honoured, case-insensitively.
    @Test
    void newAvailabilityHonoursAValidClientSuppliedInitialStatus() {
        var availability = mapper.toEntity(UUID.randomUUID(), requestWithStatus("BUSY"));

        assertThat(availability.getStatus()).isEqualTo(AvailabilityStatus.BUSY);
    }

    @Test
    void newAvailabilityAcceptsLowerCaseStatus() {
        var availability = mapper.toEntity(UUID.randomUUID(), requestWithStatus("busy"));

        assertThat(availability.getStatus()).isEqualTo(AvailabilityStatus.BUSY);
    }

    @Test
    void newAvailabilityFallsBackToAvailableForAnUnknownStatus() {
        var availability = mapper.toEntity(UUID.randomUUID(), requestWithStatus("NOT_A_STATUS"));

        assertThat(availability.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void newAvailabilityFallsBackToAvailableForABlankStatus() {
        var availability = mapper.toEntity(UUID.randomUUID(), requestWithStatus("   "));

        assertThat(availability.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    private static CreateAvailabilityRequest requestWithStatus(String status) {
        return CreateAvailabilityRequest.builder()
                .startAt(Instant.parse("2026-08-01T11:00:00Z"))
                .endAt(Instant.parse("2026-08-01T13:30:00Z"))
                .channelType("ONLINE_CHAT")
                .status(status)
                .build();
    }
}
