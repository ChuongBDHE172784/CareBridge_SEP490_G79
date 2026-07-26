package com.carebridge.backend.nearbycare.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NearbySupportRequestProjectionTest {
    @Test
    void messageProjectionRoundTripsConsentAndLifecycleTimes() {
        LocalDateTime responded = LocalDateTime.of(2026, 7, 26, 9, 0);
        LocalDateTime completed = responded.plusHours(1);
        NearbySupportRequest request = NearbySupportRequest.builder()
                .supportType("POSTPARTUM")
                .description("Need nearby help\nwith supplies")
                .consentStatus("GRANTED")
                .respondedAt(responded)
                .completedAt(completed)
                .build();

        request.prepareCanonicalMessage();
        request.setSupportType(null);
        request.setDescription(null);
        request.setConsentStatus(null);
        request.setRespondedAt(null);
        request.setCompletedAt(null);
        request.hydrateCanonicalMessage();

        assertThat(request.getSupportType()).isEqualTo("POSTPARTUM");
        assertThat(request.getDescription()).isEqualTo("Need nearby help\nwith supplies");
        assertThat(request.getConsentStatus()).isEqualTo("GRANTED");
        assertThat(request.getRespondedAt()).isEqualTo(responded);
        assertThat(request.getCompletedAt()).isEqualTo(completed);
    }

    @Test
    void migratedPlainDescriptionUsesConservativeLegacyDefaults() {
        LocalDateTime migratedAt = LocalDateTime.of(2026, 7, 22, 23, 8);
        NearbySupportRequest request = NearbySupportRequest.builder()
                .canonicalMessage("Legacy request description")
                .status(NearbySupportRequest.SupportStatus.COMPLETED)
                .updatedAt(migratedAt)
                .build();

        request.hydrateCanonicalMessage();

        assertThat(request.getDescription()).isEqualTo("Legacy request description");
        assertThat(request.getSupportType()).isEqualTo("LEGACY_UNSPECIFIED");
        assertThat(request.getConsentStatus()).isEqualTo("PENDING");
        assertThat(request.getRespondedAt()).isEqualTo(migratedAt);
        assertThat(request.getCompletedAt()).isEqualTo(migratedAt);
    }
}
