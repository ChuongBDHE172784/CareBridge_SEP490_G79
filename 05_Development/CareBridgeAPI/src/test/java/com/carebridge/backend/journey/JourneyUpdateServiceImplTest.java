package com.carebridge.backend.journey;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.impl.JourneyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC23 — UpdateMotherJourney service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class JourneyUpdateServiceImplTest {

    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;

    private JourneyServiceImpl journeyService;

    @BeforeEach
    void setUp() {
        journeyService = new JourneyServiceImpl(journeyRepository, auditService);
    }

    /** TC-023-001: Happy path — update notes + estimatedDueDate. */
    @Test
    void updateJourney_happyPath_returnsMappedResponse() {
        var journey = JourneyUpdateTestFactory.makeActiveJourney();
        var req = JourneyUpdateTestFactory.makeUpdateRequest();
        when(journeyRepository.findById(JourneyUpdateTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(journeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JourneyResponse response = journeyService.updateJourney(
                JourneyUpdateTestFactory.MOTHER_ID, JourneyUpdateTestFactory.JOURNEY_ID, req);

        assertThat(response.getJourneyId()).isEqualTo(JourneyUpdateTestFactory.JOURNEY_ID);
        assertThat(response.getNotes()).isEqualTo("Updated notes");
        assertThat(response.getEstimatedDueDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getStatus()).isEqualTo(JourneyStatus.ACTIVE.name());
        verify(auditService).log(
                eq(AuditAction.JOURNEY_UPDATED),
                eq(JourneyUpdateTestFactory.MOTHER_ID),
                eq("MotherJourney"),
                any(),
                any());
    }

    /** TC-023-002: Transition to COMPLETED with required deliveryDate. */
    @Test
    void updateJourney_completeWithDeliveryDate_setsStatusCompleted() {
        var journey = JourneyUpdateTestFactory.makeActiveJourney();
        var req = JourneyUpdateTestFactory.makeCompleteRequest();
        when(journeyRepository.findById(JourneyUpdateTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(journeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JourneyResponse response = journeyService.updateJourney(
                JourneyUpdateTestFactory.MOTHER_ID, JourneyUpdateTestFactory.JOURNEY_ID, req);

        assertThat(response.getStatus()).isEqualTo(JourneyStatus.COMPLETED.name());
        assertThat(response.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    /** TC-023-003: CRITICAL — COMPLETED without deliveryDate must be rejected (JOURNEY-013). */
    @Test
    void updateJourney_completeWithoutDeliveryDate_throwsJourney013() {
        var journey = JourneyUpdateTestFactory.makeActiveJourney();
        var req = JourneyUpdateTestFactory.makeCompleteRequestWithoutDeliveryDate();
        when(journeyRepository.findById(JourneyUpdateTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> journeyService.updateJourney(
                JourneyUpdateTestFactory.MOTHER_ID, JourneyUpdateTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("JOURNEY-013");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(journeyRepository, never()).save(any());
    }

    /**
     * TC-023-004: CRITICAL SECURITY — IDOR prevention.
     * OTHER_USER attempting to update MOTHER_ID's journey must receive 403 (JOURNEY-011).
     */
    @Test
    void updateJourney_idor_throwsJourney011() {
        var journey = JourneyUpdateTestFactory.makeActiveJourney(); // owned by MOTHER_ID
        var req = JourneyUpdateTestFactory.makeUpdateRequest();
        when(journeyRepository.findById(JourneyUpdateTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> journeyService.updateJourney(
                JourneyUpdateTestFactory.OTHER_USER_ID, JourneyUpdateTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("JOURNEY-011");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(journeyRepository, never()).save(any());
    }

    /** TC-023-005: Journey not found — must return 404 (JOURNEY-010). */
    @Test
    void updateJourney_notFound_throwsJourney010() {
        var req = JourneyUpdateTestFactory.makeUpdateRequest();
        when(journeyRepository.findById(JourneyUpdateTestFactory.UNKNOWN_JOURNEY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> journeyService.updateJourney(
                JourneyUpdateTestFactory.MOTHER_ID, JourneyUpdateTestFactory.UNKNOWN_JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("JOURNEY-010");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    /** TC-023-006: Updating a COMPLETED journey must be rejected (JOURNEY-012). */
    @Test
    void updateJourney_completedJourney_throwsJourney012() {
        var journey = JourneyUpdateTestFactory.makeCompletedJourney(); // status=COMPLETED
        var req = JourneyUpdateTestFactory.makeUpdateRequest();
        when(journeyRepository.findById(JourneyUpdateTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> journeyService.updateJourney(
                JourneyUpdateTestFactory.MOTHER_ID, JourneyUpdateTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("JOURNEY-012");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
