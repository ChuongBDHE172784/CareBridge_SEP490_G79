package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.expert.handler.IExpertEventHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies an AI disposition to the matching, still-private community content version. */
@Service
@RequiredArgsConstructor
public class AiModerationOutcomeApplier {

    public enum TargetLockResult {
        READY,
        TARGET_GONE,
        SUPERSEDED
    }

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final IExpertEventHandler expertEventHandler;

    /**
     * Serializes assessment recording and revalidates the exact private content version before
     * an assessment or moderation case can be persisted.
     */
    @Transactional
    public TargetLockResult acquireTargetLock(
            ReportTargetType targetType, UUID targetId, String expectedHash, boolean forceRescan) {
        return switch (targetType) {
            case QUESTION -> questionRepository.findByIdForModerationUpdate(targetId)
                    .map(question -> (forceRescan || question.getStatus() == QuestionStatus.AI_PENDING)
                            && question.getStatus() != QuestionStatus.DELETED
                            && AiContentHasher.sha256Hex(AiScanTargetResolver.joinTitleAndBody(
                                    question.getTitle(), question.getBody())).equals(expectedHash)
                            ? TargetLockResult.READY
                            : TargetLockResult.SUPERSEDED)
                    .orElse(TargetLockResult.TARGET_GONE);
            case ANSWER -> answerRepository.findByIdForModerationUpdate(targetId)
                    .map(answer -> (forceRescan || answer.getStatus() == AnswerStatus.AI_PENDING)
                            && answer.getStatus() != AnswerStatus.DELETED
                            && AiContentHasher.sha256Hex(answer.getBody()).equals(expectedHash)
                            ? TargetLockResult.READY
                            : TargetLockResult.SUPERSEDED)
                    .orElse(TargetLockResult.TARGET_GONE);
            default -> TargetLockResult.READY; // Library CONTENT keeps its existing lifecycle.
        };
    }

    @Transactional
    public void applyCompleted(AiContentAssessment assessment) {
        boolean safe = assessment.getClassification() == AiClassification.SAFE;
        apply(assessment.getTargetType(), assessment.getTargetId(), assessment.getContentHash(), safe);
    }

    @Transactional
    public void applyHumanReview(ReportTargetType targetType, UUID targetId, String contentHash) {
        apply(targetType, targetId, contentHash, false);
    }

    private void apply(ReportTargetType targetType, UUID targetId, String expectedHash, boolean safe) {
        switch (targetType) {
            case QUESTION -> applyQuestion(targetId, expectedHash, safe);
            case ANSWER -> applyAnswer(targetId, expectedHash, safe);
            default -> {
                // The AI-first publication state machine is scoped to community Q&A.
            }
        }
    }

    private void applyQuestion(UUID questionId, String expectedHash, boolean safe) {
        CommunityQuestion question = questionRepository.findByIdForModerationUpdate(questionId).orElse(null);
        if (question == null || question.getStatus() != QuestionStatus.AI_PENDING) {
            return;
        }
        String currentText = AiScanTargetResolver.joinTitleAndBody(question.getTitle(), question.getBody());
        if (!AiContentHasher.sha256Hex(currentText).equals(expectedHash)) {
            return;
        }
        question.setStatus(safe ? QuestionStatus.APPROVED : QuestionStatus.PENDING);
        questionRepository.save(question);
    }

    private void applyAnswer(UUID answerId, String expectedHash, boolean safe) {
        CommunityAnswer answer = answerRepository.findByIdForModerationUpdate(answerId).orElse(null);
        if (answer == null || answer.getStatus() != AnswerStatus.AI_PENDING) {
            return;
        }
        if (!AiContentHasher.sha256Hex(answer.getBody()).equals(expectedHash)) {
            return;
        }
        boolean parentStillApproved = safe && questionRepository
                .findByIdForModerationUpdate(answer.getQuestionId())
                .map(question -> question.getStatus() == QuestionStatus.APPROVED)
                .orElse(false);
        boolean approve = safe && parentStillApproved;
        answer.setStatus(approve ? AnswerStatus.APPROVED : AnswerStatus.PENDING);
        answerRepository.save(answer);
        if (!approve) {
            return;
        }
        questionRepository.incrementAnswerCount(answer.getQuestionId());
        if (answer.isExpertLabeled()) {
            expertEventHandler.onAnswerApproved(answer.getId().toString(), answer.getAuthorId().toString());
        }
    }
}
