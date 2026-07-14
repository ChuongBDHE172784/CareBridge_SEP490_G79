package com.carebridge.backend.community.policy;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.policy.TriageRedFlagPolicy;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunitySafetyPolicy {

    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final ContentReportRepository contentReportRepository;
    private final TriageRedFlagPolicy redFlagPolicy;
    private final AuditService auditService;

    public User requirePostingAllowed(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
        Instant restrictedUntil = user.getCommunityPostingRestrictedUntil();
        if (restrictedUntil != null && restrictedUntil.isAfter(Instant.now())) {
            throw new AccessDeniedException("Community posting is restricted until " + restrictedUntil);
        }
        return user;
    }

    public boolean isVerifiedActiveExpert(User user) {
        if (user == null || user.getRole() != Role.EXPERT || !user.isEnabled() || user.isLocked()) {
            return false;
        }
        Instant suspendedUntil = user.getSuspendedUntil();
        if (suspendedUntil != null && suspendedUntil.isAfter(Instant.now())) {
            return false;
        }
        return expertProfileRepository.findByUserId(user.getId())
                .filter(profile -> profile.getVerificationStatus() == VerificationStatus.APPROVED)
                .isPresent();
    }

    public CommunityQuestion requireVisibleQuestion(UUID userId, UUID questionId) {
        return questionRepository.findById(questionId)
                .filter(q -> isQuestionVisibleTo(q, userId))
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));
    }

    public CommunityAnswer requireVisibleAnswer(UUID userId, UUID answerId) {
        CommunityAnswer answer = answerRepository.findById(answerId)
                .filter(a -> a.getStatus() == AnswerStatus.APPROVED)
                .orElseThrow(() -> new AnswerNotFoundException(answerId.toString()));
        requireVisibleQuestion(userId, answer.getQuestionId());
        return answer;
    }

    public boolean isQuestionVisibleTo(CommunityQuestion question, UUID userId) {
        return question.getStatus() == QuestionStatus.APPROVED
                || (question.getStatus() == QuestionStatus.PENDING && question.getAuthorId().equals(userId));
    }

    public void autoReportIfRedFlag(UUID reporterUserId, UUID targetId, ReportTargetType targetType, String text) {
        if (text == null || text.isBlank() || !redFlagPolicy.isRedFlag(text)) {
            return;
        }
        boolean duplicatePending = contentReportRepository.existsByReporterUserIdAndTargetIdAndStatus(
                reporterUserId, targetId, ReportStatus.PENDING);
        if (duplicatePending) {
            return;
        }
        ContentReport report = ContentReport.builder()
                .targetId(targetId)
                .targetType(targetType)
                .status(ReportStatus.PENDING)
                .category(ReportCategory.UNSAFE_ADVICE.name())
                .reportSource(ReportSource.AUTOMATED)
                .description("Auto-flagged by community red-flag policy")
                .reporterUserId(reporterUserId)
                .createdAt(Instant.now())
                .build();
        ContentReport saved = contentReportRepository.save(report);
        auditService.log(AuditAction.CONTENT_REPORTED, reporterUserId, "ContentReport",
                saved.getId().toString(),
                "auto=true targetType=" + targetType + " targetId=" + targetId);
    }
}
