package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.repository.PostpartumLogRepository;
import com.carebridge.backend.health.service.PostpartumAiAnalyzer;
import com.carebridge.backend.health.service.impl.PostpartumLogServiceImpl;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
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
    @InjectMocks private PostpartumLogServiceImpl postpartumLogService;

    /** POST-TC-028-001: Happy path — valid POSTPARTUM journey, log saved + audit emitted. */
    @Test
    void addLog_validPostpartumJourney_returnsLogResponse() {
        var journey = PostpartumLogTestFactory.makeActivePostpartumJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
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
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

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
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

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

    /** POST-TC-028-006: CRITICAL SECURITY — IDOR: journey not owned by user → POST-006 (403). */
    @Test
    void addLog_journeyNotOwned_throwsPost006() {
        var journey = PostpartumLogTestFactory.makeOtherUsersJourney();
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> postpartumLogService.addLog(
                PostpartumLogTestFactory.MOTHER_ID, PostpartumLogTestFactory.JOURNEY_ID, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("POST-006");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(logRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** POST-TC-028-001 (journey not found): Journey not found → POST-001 (404). */
    @Test
    void addLog_journeyNotFound_throwsPost001() {
        var req = PostpartumLogTestFactory.makeValidRequest();
        when(journeyRepository.findById(PostpartumLogTestFactory.UNKNOWN_JOURNEY)).thenReturn(Optional.empty());

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
        when(journeyRepository.findById(PostpartumLogTestFactory.JOURNEY_ID)).thenReturn(Optional.of(journey));
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
}
