package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import com.carebridge.backend.health.event.PostpartumLogDeleted;
import com.carebridge.backend.health.event.PostpartumLogUpdated;
import com.carebridge.backend.health.repository.PostpartumLogRepository;
import com.carebridge.backend.health.service.PostpartumAiAnalyzer;
import com.carebridge.backend.health.service.impl.PostpartumLogServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC28 — AddPostpartumLog service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class PostpartumLogServiceTest {

    @Mock private PostpartumLogRepository logRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @Mock private PostpartumAiAnalyzer postpartumAiAnalyzer;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private LifecycleConsentValidator consentValidator;
    @InjectMocks private PostpartumLogServiceImpl postpartumLogService;

    /** POST-TC-028-001: Happy path — valid POSTPARTUM journey, log saved + audit emitted. */
    @Test
    void addLog_validPostpartumJourney_returnsLogResponse() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(logRepository.save(any())).thenAnswer(inv -> {
            PostpartumLog log = inv.getArgument(0);
            log.setId(java.util.UUID.randomUUID());
            return log;
        });
        when(postpartumAiAnalyzer.analyze(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new PostpartumAiAnalyzer.InsightResult("Recovery is progressing well", false)));

        PostpartumLogResponse response = postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req);

        assertThat(response.getPostpartumLogId()).isNotNull();
        assertThat(response.getJourneyId()).isEqualTo(PostpartumLogTestFactory.JOURNEY_ID);
        assertThat(response.getLogDate()).isEqualTo(req.getLogDate());
        assertThat(response.getPainLevel()).isEqualTo((short) 3);
        verify(logRepository).save(any());
        verify(auditService).log(eq(AuditAction.POSTPARTUM_LOG_ADDED), eq(PostpartumLogTestFactory.MOTHER_ID),
                eq("PostpartumLog"), any(), any());
    }

    /** POST-TC-028-002: CRITICAL — Journey type is PREGNANCY, not POSTPARTUM → POST-002 (400). */
    @Test
    void addLog_pregnancyJourney_throwsPost002() {
        var journey = PostpartumLogTestFactory.makePregnancyJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("POST-002");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(logRepository, never()).save(any());
    }

    /** POST-TC-028-003: CRITICAL — Journey is COMPLETED (not ACTIVE) → POST-003 (400). */
    @Test
    void addLog_completedJourney_throwsPost003() {
        var journey = PostpartumLogTestFactory.makeCompletedJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("POST-003");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(logRepository, never()).save(any());
    }

    /** POST-TC-028-006: CRITICAL SECURITY — foreign and missing journeys are indistinguishable. */
    @Test
    void addLog_journeyNotOwned_throwsPost006() {
        var journey = PostpartumLogTestFactory.makeOtherUsersJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("POST-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** POST-TC-028-001 (journey not found): Journey not found → POST-001 (404). */
    @Test
    void addLog_journeyNotFound_throwsPost001() {
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.UNKNOWN_JOURNEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.UNKNOWN_JOURNEY, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("POST-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(logRepository, never()).save(any());
    }

    /** POST-TC-028-007: Gemini AI fails → log still saved (fail-open), aiInsight=null. */
    @Test
    void addLog_aiFails_logSavedWithNullInsight() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(logRepository.save(any())).thenAnswer(inv -> {
            PostpartumLog log = inv.getArgument(0);
            log.setId(java.util.UUID.randomUUID());
            return log;
        });
        when(postpartumAiAnalyzer.analyze(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AI timeout")));

        PostpartumLogResponse response = postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req);

        assertThat(response.getPostpartumLogId()).isNotNull();
        assertThat(response.getAiInsight()).isNull();
        assertThat(response.isRedFlagAlert()).isFalse();
        verify(logRepository).save(any());
    }

    @Test
    void listLogs_ownerReturnsActiveLogs() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var log = PostpartumLogTestFactory.makeActiveLog();
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndStatus(eq(
                PostpartumLogTestFactory.JOURNEY_ID), eq(PostpartumLogStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(log)));

        var response = postpartumLogService.listLogs(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.MOTHER_ID, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPostpartumLogId()).isEqualTo(PostpartumLogTestFactory.LOG_ID);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void listLogs_equalDateAndCreatedAtUsesIdAsStablePageBoundary() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndStatus(eq(
                PostpartumLogTestFactory.JOURNEY_ID), eq(PostpartumLogStatus.ACTIVE), any()))
                .thenReturn(Page.empty());

        postpartumLogService.listLogs(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.MOTHER_ID, 1, 20);

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(logRepository).findByJourneyIdAndStatus(
                eq(PostpartumLogTestFactory.JOURNEY_ID), eq(PostpartumLogStatus.ACTIVE), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().toList()).containsExactly(
                new Sort.Order(Sort.Direction.DESC, "logDate"),
                new Sort.Order(Sort.Direction.DESC, "createdAt"),
                new Sort.Order(Sort.Direction.DESC, "id"));
    }

    @Test
    void addLog_sameSubmissionAndPayload_replaysWithoutSecondSave() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var request = PostpartumLogTestFactory.makeValidRequest();
        var existing = PostpartumLogTestFactory.makeActiveLog();
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndSubmissionId(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.SUBMISSION_ID))
                .thenReturn(Optional.of(existing));

        var response = postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, request);

        assertThat(response.getPostpartumLogId()).isEqualTo(PostpartumLogTestFactory.LOG_ID);
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void addLog_sameSubmissionCanonicalEquivalentPayload_replays() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var request = PostpartumLogTestFactory.makeValidRequest();
        request.setSleepHours(new java.math.BigDecimal("6.50"));
        request.setBreastfeedingNote("  Fed 4 times, baby latched well ");
        request.setSymptomNote(" Mild cramps, improving  ");
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndSubmissionId(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.SUBMISSION_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActiveLog()));

        var response = postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, request);

        assertThat(response.getPostpartumLogId()).isEqualTo(PostpartumLogTestFactory.LOG_ID);
        verify(logRepository, never()).save(any());
    }

    @Test
    void addLog_sameSubmissionDifferentPayload_throwsConflict() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var request = PostpartumLogTestFactory.makeValidRequest();
        request.setPainLevel((short) 9);
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndSubmissionId(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.SUBMISSION_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActiveLog()));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("POSTPARTUM_SUBMISSION_CONFLICT"));
    }

    @Test
    void addLog_sameSubmissionDeletedLog_throwsStableConflict() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var request = PostpartumLogTestFactory.makeValidRequest();
        var deleted = PostpartumLogTestFactory.makeActiveLog();
        deleted.setStatus(PostpartumLogStatus.DELETED);
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(journey));
        when(logRepository.findByJourneyIdAndSubmissionId(
                PostpartumLogTestFactory.JOURNEY_ID, PostpartumLogTestFactory.SUBMISSION_ID))
                .thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException business = (BusinessException) error;
                    assertThat(business.getCode()).isEqualTo("POSTPARTUM_SUBMISSION_GONE");
                    assertThat(business.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void getLogDetail_deletedOrMissing_throwsPplog001() {
        when(logRepository.findByIdAndStatus(PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postpartumLogService.getLogDetail(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("PPLOG-001"));
        var order = inOrder(consentValidator, logRepository);
        order.verify(consentValidator).ensureEligibleForRead(PostpartumLogTestFactory.MOTHER_ID);
        order.verify(logRepository).findByIdAndStatus(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE);
    }

    @Test
    void getLogDetail_notOwner_returnsNeutralNotFound() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        when(logRepository.findByIdAndStatus(PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeOtherUsersJourney()));

        assertThatThrownBy(() -> postpartumLogService.getLogDetail(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("PPLOG-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void updateLog_partialUpdateChangesOnlyPresentFields() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        var request = PostpartumLogTestFactory.makeUpdateRequest();
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActivePostpartumJourney()));
        when(logRepository.save(any(PostpartumLog.class))).thenAnswer(inv -> inv.getArgument(0));

        PostpartumLogResponse response = postpartumLogService.updateLog(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID, request);

        assertThat(response.getPainLevel()).isEqualTo((short) 4);
        assertThat(response.getLogDate()).isEqualTo(request.getLogDate());
        assertThat(response.getMoodLevel()).isEqualTo((short) 7);
        assertThat(response.getSymptomNote()).isEqualTo("Pain improved");
        verify(eventPublisher).publishEvent(any(PostpartumLogUpdated.class));
    }

    @Test
    void updateLog_explicitBlankOptionalNote_clearsStoredValue() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        var request = new com.carebridge.backend.health.dto.UpdatePostpartumLogRequest();
        request.setSymptomNote("   ");
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActivePostpartumJourney()));
        when(logRepository.save(any(PostpartumLog.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = postpartumLogService.updateLog(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID, request);

        assertThat(response.getSymptomNote()).isNull();
        verify(eventPublisher).publishEvent(any(PostpartumLogUpdated.class));
    }

    @Test
    void updateLog_emptyRequest_rejectsBeforeSaveAuditOrEvent() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        var request = new com.carebridge.backend.health.dto.UpdatePostpartumLogRequest();
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActivePostpartumJourney()));

        assertThatThrownBy(() -> postpartumLogService.updateLog(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException business = (BusinessException) error;
                    assertThat(business.getCode()).isEqualTo("PPLOG-004");
                    assertThat(business.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(PostpartumLogUpdated.class));
    }

    @Test
    void updateLog_sameValues_rejectsBeforeSaveAuditOrEvent() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        var request = new com.carebridge.backend.health.dto.UpdatePostpartumLogRequest();
        request.setPainLevel(log.getPainLevel());
        request.setSleepHours(new java.math.BigDecimal("6.50"));
        request.setSymptomNote("  Mild cramps, improving  ");
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActivePostpartumJourney()));

        assertThatThrownBy(() -> postpartumLogService.updateLog(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("PPLOG-004"));
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(PostpartumLogUpdated.class));
    }

    @Test
    void deleteLog_ownerSoftDeletesAndPublishesMinimumEvent() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeActivePostpartumJourney()));
        when(logRepository.save(any(PostpartumLog.class))).thenAnswer(inv -> inv.getArgument(0));

        postpartumLogService.deleteLog(PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID);

        assertThat(log.getStatus()).isEqualTo(PostpartumLogStatus.DELETED);
        verify(logRepository).save(log);
        verify(logRepository, never()).delete(any());
        verify(logRepository, never()).deleteById(any());
        verify(eventPublisher).publishEvent(any(PostpartumLogDeleted.class));
    }

    @Test
    void deleteLog_notOwnerDoesNotMutate() {
        var log = PostpartumLogTestFactory.makeActiveLog();
        when(logRepository.findByIdAndStatusForUpdate(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogStatus.ACTIVE))
                .thenReturn(Optional.of(log));
        when(journeyRepository.findByIdForUpdate(PostpartumLogTestFactory.JOURNEY_ID))
                .thenReturn(Optional.of(PostpartumLogTestFactory.makeOtherUsersJourney()));

        assertThatThrownBy(() -> postpartumLogService.deleteLog(
                PostpartumLogTestFactory.LOG_ID, PostpartumLogTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("PPLOG-001"));

        assertThat(log.getStatus()).isEqualTo(PostpartumLogStatus.ACTIVE);
        verify(logRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
