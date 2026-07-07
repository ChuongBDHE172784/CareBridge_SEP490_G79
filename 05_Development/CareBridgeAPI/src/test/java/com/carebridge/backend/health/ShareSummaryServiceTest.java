package com.carebridge.backend.health;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.health.dto.ShareSummaryRequest;
import com.carebridge.backend.health.entity.ConsultationBooking;
import com.carebridge.backend.health.entity.HealthSummary;
import com.carebridge.backend.health.repository.ConsultationBookingRepository;
import com.carebridge.backend.health.repository.DataPermissionRepository;
import com.carebridge.backend.health.repository.HealthSummaryRepository;
import com.carebridge.backend.health.service.impl.ShareSummaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareSummaryServiceTest {

    static final UUID MOTHER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_MOTHER   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID SUMMARY_ID     = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID BOOKING_ID     = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    static final UUID EXPERT_PROF_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    @Mock private HealthSummaryRepository summaryRepository;
    @Mock private ConsultationBookingRepository bookingRepository;
    @Mock private DataPermissionRepository permissionRepository;
    @Mock private AuditService auditService;
    @InjectMocks private ShareSummaryServiceImpl service;

    private HealthSummary makeSummary(UUID ownerId) {
        return HealthSummary.builder()
                .id(SUMMARY_ID)
                .ownerUserId(ownerId)
                .summaryPeriod("24H")
                .periodStart(LocalDate.now().minusDays(1))
                .periodEnd(LocalDate.now())
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
    }

    private ConsultationBooking makeActiveBooking() {
        var b = new ConsultationBooking();
        b.setId(BOOKING_ID);
        b.setRequesterUserId(MOTHER_ID);
        b.setExpertProfileId(EXPERT_PROF_ID);
        b.setStatus("CONFIRMED");
        return b;
    }

    // SHARE-TC-001: valid triple gate → returns ShareSummaryResponse
    @Test
    void shareSummary_validGate_returnsResponse() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, MOTHER_ID))
                .thenReturn(Optional.of(makeSummary(MOTHER_ID)));
        when(bookingRepository.findActiveByIdAndRequester(BOOKING_ID, MOTHER_ID))
                .thenReturn(Optional.of(makeActiveBooking()));
        when(permissionRepository.existsValidPermission(eq(MOTHER_ID), any(), any()))
                .thenReturn(true);

        var response = service.shareSummary(new ShareSummaryRequest(SUMMARY_ID, BOOKING_ID), MOTHER_ID);

        assertThat(response).isNotNull();
        assertThat(response.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.summaryId()).isEqualTo(SUMMARY_ID);
    }

    // SHARE-TC-002: summary not owned → HEALTH-007 / ResourceNotFoundException
    @Test
    void shareSummary_summaryNotOwned_throws() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, OTHER_MOTHER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.shareSummary(new ShareSummaryRequest(SUMMARY_ID, BOOKING_ID), OTHER_MOTHER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // SHARE-TC-003: booking not active / not owned → HEALTH-008 / BusinessException
    @Test
    void shareSummary_bookingNotActive_throws() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, MOTHER_ID))
                .thenReturn(Optional.of(makeSummary(MOTHER_ID)));
        when(bookingRepository.findActiveByIdAndRequester(any(), eq(MOTHER_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.shareSummary(new ShareSummaryRequest(SUMMARY_ID, UUID.randomUUID()), MOTHER_ID))
                .isInstanceOf(BusinessException.class);
    }

    // SHARE-TC-004: no data permission → HEALTH-009 / BusinessException
    @Test
    void shareSummary_noDataPermission_throws() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, MOTHER_ID))
                .thenReturn(Optional.of(makeSummary(MOTHER_ID)));
        when(bookingRepository.findActiveByIdAndRequester(BOOKING_ID, MOTHER_ID))
                .thenReturn(Optional.of(makeActiveBooking()));
        when(permissionRepository.existsValidPermission(any(), any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.shareSummary(new ShareSummaryRequest(SUMMARY_ID, BOOKING_ID), MOTHER_ID))
                .isInstanceOf(BusinessException.class);
    }
}
