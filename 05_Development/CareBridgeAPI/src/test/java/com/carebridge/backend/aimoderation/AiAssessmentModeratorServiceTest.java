package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.dto.request.AiFeedbackRequest;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentResponse;
import com.carebridge.backend.aimoderation.dto.response.AiFeedbackResponse;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.service.AiAssessmentModeratorService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CB-MOD-IMP-017: current feedback lives on moderation_cases (no join table); every submit
 * appends an immutable AI_FEEDBACK_SUBMITTED moderation event in the same transaction.
 */
@ExtendWith(MockitoExtension.class)
class AiAssessmentModeratorServiceTest {

    @Mock
    private AiContentAssessmentRepository assessmentRepository;
    @Mock
    private ContentReportRepository contentReportRepository;
    @Mock
    private ModerationActionRepository moderationActionRepository;
    @Mock
    private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiModerationMapper mapper = new AiModerationMapper(new ObjectMapper());

    private AiAssessmentModeratorService service;

    private static final UUID MODERATOR_ID = UUID.randomUUID();
    private static final UUID OTHER_MODERATOR_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();

    private AiContentAssessment assessment;
    private ContentReport moderationCase;

    @BeforeEach
    void setUp() {
        service = new AiAssessmentModeratorService(assessmentRepository, contentReportRepository,
                moderationActionRepository, mapper, auditService, objectMapper);
        assessment = AiContentAssessment.builder()
                .id(ASSESSMENT_ID)
                .targetType(ReportTargetType.QUESTION)
                .targetId(UUID.randomUUID())
                .contentHash("hash")
                .policySetHash("psh")
                .model("gemini-1.5-flash")
                .status(AiAssessmentStatus.COMPLETED)
                .classification(AiClassification.VIOLATION)
                .confidence(new BigDecimal("0.9"))
                .matchesJson("[]")
                .moderationCaseId(CASE_ID)
                .createdAt(Instant.now())
                .build();
        moderationCase = ContentReport.builder()
                .id(CASE_ID)
                .targetId(assessment.getTargetId())
                .targetType(ReportTargetType.QUESTION)
                .status(ReportStatus.PENDING)
                .reportSource(ReportSource.AUTOMATED)
                .category("SPAM")
                .createdAt(Instant.now())
                .build();
    }

    private void givenAssessmentAndCase() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));
        when(contentReportRepository.findById(CASE_ID)).thenReturn(Optional.of(moderationCase));
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> {
            ModerationAction action = inv.getArgument(0);
            action.setId(UUID.randomUUID());
            return action;
        });
    }

    // VII.6: AGREE updates the case's current-feedback columns
    @Test
    void feedbackAgree_updatesModerationCaseColumns() {
        givenAssessmentAndCase();

        AiFeedbackResponse response = service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.AGREE, "đúng như AI nói"), MODERATOR_ID);

        assertThat(moderationCase.getAiFeedbackDecision()).isEqualTo(AiFeedbackVerdict.AGREE);
        assertThat(moderationCase.getAiFeedbackReason()).isEqualTo("đúng như AI nói");
        assertThat(moderationCase.getAiFeedbackBy()).isEqualTo(MODERATOR_ID);
        assertThat(moderationCase.getAiFeedbackAt()).isNotNull();
        assertThat(moderationCase.getAiFeedbackAssessmentId()).isEqualTo(ASSESSMENT_ID);
        assertThat(response.verdict()).isEqualTo(AiFeedbackVerdict.AGREE);
    }

    // VII.7 + VII.9: DISAGREE appends an immutable moderation event with a sanitized payload
    @Test
    void feedbackDisagree_appendsModerationEventWithSanitizedPayload() {
        givenAssessmentAndCase();

        service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.DISAGREE, "AI nhầm — đây là mô tả triệu chứng"),
                MODERATOR_ID);

        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(captor.capture());
        ModerationAction event = captor.getValue();
        assertThat(event.getActionType()).isEqualTo(ModerationActionType.AI_FEEDBACK_SUBMITTED);
        assertThat(event.getReportId()).isEqualTo(CASE_ID);
        assertThat(event.getModeratorUserId()).isEqualTo(MODERATOR_ID);
        assertThat(event.getEventPayloadJson()).contains("\"decision\":\"DISAGREE\"");
        assertThat(event.getEventPayloadJson()).contains(ASSESSMENT_ID.toString());
        verify(auditService).log(eq(AuditAction.AI_FEEDBACK_SUBMITTED), eq(MODERATOR_ID),
                eq("AiContentAssessment"), eq(ASSESSMENT_ID.toString()), any());
    }

    // VII.8 + VII.9: a second submission replaces the current feedback but appends a SECOND
    // event carrying previousDecision — history is never lost
    @Test
    void secondFeedback_replacesCurrent_andRecordsPreviousDecisionInHistory() {
        givenAssessmentAndCase();

        service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.AGREE, null), MODERATOR_ID);
        service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.DISAGREE, "xem lại kỹ hơn"), MODERATOR_ID);

        assertThat(moderationCase.getAiFeedbackDecision()).isEqualTo(AiFeedbackVerdict.DISAGREE);
        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<ModerationAction> events = captor.getAllValues();
        assertThat(events.get(0).getEventPayloadJson()).doesNotContain("previousDecision");
        assertThat(events.get(1).getEventPayloadJson()).contains("\"previousDecision\":\"AGREE\"");
    }

    // VII.11: an assessment that never got attached to a case cannot receive feedback
    @Test
    void feedback_onUnattachedAssessment_isRejected() {
        assessment.setModerationCaseId(null);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.AGREE, null), MODERATOR_ID))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-013");
        verify(contentReportRepository, never()).save(any());
        verify(moderationActionRepository, never()).save(any());
    }

    // VII.10-adjacent: a case actively reviewed by another moderator rejects foreign feedback
    @Test
    void feedback_whileClaimedByAnotherModerator_isRejected() {
        moderationCase.setStatus(ReportStatus.IN_REVIEW);
        moderationCase.setAssignedModeratorId(OTHER_MODERATOR_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));
        when(contentReportRepository.findById(CASE_ID)).thenReturn(Optional.of(moderationCase));

        assertThatThrownBy(() -> service.submitFeedback(ASSESSMENT_ID,
                new AiFeedbackRequest(AiFeedbackVerdict.AGREE, null), MODERATOR_ID))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-038");
        verify(moderationActionRepository, never()).save(any());
    }

    // VII.5-adjacent: the assessment view exposes the case-held feedback only when it refers
    // to THIS assessment (typed contract preserved for the frontend)
    @Test
    void assessmentView_buildsFeedbackFromCaseColumns() {
        moderationCase.setAiFeedbackDecision(AiFeedbackVerdict.DISAGREE);
        moderationCase.setAiFeedbackReason("không phải vi phạm");
        moderationCase.setAiFeedbackAssessmentId(ASSESSMENT_ID);
        when(assessmentRepository.findFirstByModerationCaseIdOrderByCreatedAtDesc(CASE_ID))
                .thenReturn(Optional.of(assessment));
        when(contentReportRepository.findById(CASE_ID)).thenReturn(Optional.of(moderationCase));

        AiAssessmentResponse response = service.getAssessmentForReport(CASE_ID, MODERATOR_ID);

        assertThat(response.myFeedbackVerdict()).isEqualTo(AiFeedbackVerdict.DISAGREE);
        assertThat(response.myFeedbackNote()).isEqualTo("không phải vi phạm");
    }

    @Test
    void assessmentView_hidesFeedbackBelongingToAnotherAssessment() {
        moderationCase.setAiFeedbackDecision(AiFeedbackVerdict.AGREE);
        moderationCase.setAiFeedbackAssessmentId(UUID.randomUUID()); // feedback on an older assessment
        when(assessmentRepository.findFirstByModerationCaseIdOrderByCreatedAtDesc(CASE_ID))
                .thenReturn(Optional.of(assessment));
        when(contentReportRepository.findById(CASE_ID)).thenReturn(Optional.of(moderationCase));

        AiAssessmentResponse response = service.getAssessmentForReport(CASE_ID, MODERATOR_ID);

        assertThat(response.myFeedbackVerdict()).isNull();
        assertThat(response.myFeedbackNote()).isNull();
    }
}
