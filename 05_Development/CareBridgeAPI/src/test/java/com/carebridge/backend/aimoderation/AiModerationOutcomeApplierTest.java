package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.service.AiModerationOutcomeApplier;
import com.carebridge.backend.aimoderation.service.AiModerationOutcomeApplier.TargetLockResult;
import com.carebridge.backend.aimoderation.service.AiScanTargetResolver;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.expert.handler.IExpertEventHandler;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiModerationOutcomeApplierTest {

    @Mock private CommunityQuestionRepository questionRepository;
    @Mock private CommunityAnswerRepository answerRepository;
    @Mock private IExpertEventHandler expertEventHandler;

    private AiModerationOutcomeApplier applier;
    private UUID questionId;
    private UUID answerId;

    @BeforeEach
    void setUp() {
        applier = new AiModerationOutcomeApplier(questionRepository, answerRepository, expertEventHandler);
        questionId = UUID.randomUUID();
        answerId = UUID.randomUUID();
    }

    @Test
    void safeQuestion_matchingHash_isApproved() {
        CommunityQuestion question = question(QuestionStatus.AI_PENDING, "Title", "Body");
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.of(question));

        applier.applyCompleted(assessment(ReportTargetType.QUESTION, questionId,
                hashQuestion(question), AiClassification.SAFE));

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.APPROVED);
        verify(questionRepository).save(question);
    }

    @Test
    void missingTarget_cannotAcquireOutcomeLock() {
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.empty());

        TargetLockResult acquired = applier.acquireTargetLock(
                ReportTargetType.QUESTION, questionId, "hash", false);

        assertThat(acquired).isEqualTo(TargetLockResult.TARGET_GONE);
    }

    @Test
    void editedTarget_cannotAcquireOutcomeLockForOldHash() {
        CommunityQuestion question = question(QuestionStatus.AI_PENDING, "Edited", "New body");
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.of(question));

        TargetLockResult acquired = applier.acquireTargetLock(
                ReportTargetType.QUESTION, questionId, AiContentHasher.sha256Hex("Old\nBody"), false);

        assertThat(acquired).isEqualTo(TargetLockResult.SUPERSEDED);
    }

    @Test
    void forcedRescan_approvedQuestion_canAcquireMatchingVersionLock() {
        CommunityQuestion question = question(QuestionStatus.APPROVED, "Title", "Body");
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.of(question));

        TargetLockResult acquired = applier.acquireTargetLock(
                ReportTargetType.QUESTION, questionId, hashQuestion(question), true);

        assertThat(acquired).isEqualTo(TargetLockResult.READY);
    }

    @Test
    void nonSafeQuestion_isSentToHumanReviewEvenWithoutCase() {
        CommunityQuestion question = question(QuestionStatus.AI_PENDING, "Title", "Body");
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.of(question));

        applier.applyCompleted(assessment(ReportTargetType.QUESTION, questionId,
                hashQuestion(question), AiClassification.UNCERTAIN));

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    @Test
    void safeExpertAnswer_isApprovedCountedAndRewardedOnce() {
        UUID authorId = UUID.randomUUID();
        CommunityAnswer answer = CommunityAnswer.builder()
                .id(answerId).questionId(questionId).authorId(authorId).body("Safe answer")
                .expertLabeled(true).status(AnswerStatus.AI_PENDING).build();
        when(answerRepository.findByIdForModerationUpdate(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findByIdForModerationUpdate(questionId))
                .thenReturn(Optional.of(question(QuestionStatus.APPROVED, "Question", "Body")));

        AiContentAssessment assessment = assessment(ReportTargetType.ANSWER, answerId,
                AiContentHasher.sha256Hex(answer.getBody()), AiClassification.SAFE);
        applier.applyCompleted(assessment);
        applier.applyCompleted(assessment);

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.APPROVED);
        verify(questionRepository).incrementAnswerCount(questionId);
        verify(expertEventHandler).onAnswerApproved(answerId.toString(), authorId.toString());
    }

    @Test
    void safeAnswer_parentNoLongerApproved_isSentToHumanWithoutCount() {
        CommunityAnswer answer = CommunityAnswer.builder()
                .id(answerId).questionId(questionId).authorId(UUID.randomUUID()).body("Safe answer")
                .status(AnswerStatus.AI_PENDING).build();
        when(answerRepository.findByIdForModerationUpdate(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findByIdForModerationUpdate(questionId))
                .thenReturn(Optional.of(question(QuestionStatus.HIDDEN, "Question", "Body")));

        applier.applyCompleted(assessment(ReportTargetType.ANSWER, answerId,
                AiContentHasher.sha256Hex(answer.getBody()), AiClassification.SAFE));

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
        verify(questionRepository, never()).incrementAnswerCount(any());
        verify(expertEventHandler, never()).onAnswerApproved(any(), any());
    }

    @Test
    void terminalFailure_matchingHash_routesAnswerToHumanReview() {
        CommunityAnswer answer = CommunityAnswer.builder()
                .id(answerId).questionId(questionId).authorId(UUID.randomUUID()).body("Answer")
                .status(AnswerStatus.AI_PENDING).build();
        when(answerRepository.findByIdForModerationUpdate(answerId)).thenReturn(Optional.of(answer));

        applier.applyHumanReview(ReportTargetType.ANSWER, answerId,
                AiContentHasher.sha256Hex(answer.getBody()));

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
        verify(questionRepository, never()).incrementAnswerCount(any());
    }

    @Test
    void staleResult_cannotApproveEditedContent() {
        CommunityQuestion question = question(QuestionStatus.AI_PENDING, "Edited", "New body");
        when(questionRepository.findByIdForModerationUpdate(questionId)).thenReturn(Optional.of(question));

        applier.applyCompleted(assessment(ReportTargetType.QUESTION, questionId,
                AiContentHasher.sha256Hex("Old\nBody"), AiClassification.SAFE));

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.AI_PENDING);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void moderatorAction_isNeverOverwritten() {
        CommunityAnswer answer = CommunityAnswer.builder()
                .id(answerId).questionId(questionId).authorId(UUID.randomUUID()).body("Answer")
                .status(AnswerStatus.HIDDEN).build();
        when(answerRepository.findByIdForModerationUpdate(answerId)).thenReturn(Optional.of(answer));

        applier.applyCompleted(assessment(ReportTargetType.ANSWER, answerId,
                AiContentHasher.sha256Hex(answer.getBody()), AiClassification.SAFE));

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.HIDDEN);
        verify(answerRepository, never()).save(any());
        verify(questionRepository, never()).incrementAnswerCount(any());
    }

    private CommunityQuestion question(QuestionStatus status, String title, String body) {
        return CommunityQuestion.builder().id(questionId).authorId(UUID.randomUUID())
                .title(title).body(body).status(status).build();
    }

    private String hashQuestion(CommunityQuestion question) {
        return AiContentHasher.sha256Hex(
                AiScanTargetResolver.joinTitleAndBody(question.getTitle(), question.getBody()));
    }

    private AiContentAssessment assessment(ReportTargetType targetType, UUID targetId,
            String contentHash, AiClassification classification) {
        return AiContentAssessment.builder().id(UUID.randomUUID()).targetType(targetType)
                .targetId(targetId).contentHash(contentHash).classification(classification).build();
    }
}
