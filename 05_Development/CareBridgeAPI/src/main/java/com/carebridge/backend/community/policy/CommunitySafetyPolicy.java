package com.carebridge.backend.community.policy;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Community posting gates and visibility rules ONLY. The former autoReportIfRedFlag() hook
 * (medical red-flag keyword -> AUTOMATED ContentReport, category UNSAFE_ADVICE) was removed
 * (CB-MOD-IMP-017): a user DESCRIBING symptoms is a medical-safety signal, not a content
 * violation. Emergency keyword handling stays in TriageRedFlagPolicy (triage/RAG routing,
 * untouched); content violations are detected semantically by the AI moderation scan that is
 * enqueued in the same transactions that used to call the removed hook.
 */
@Component
@RequiredArgsConstructor
public class CommunitySafetyPolicy {

    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final ModerationActionRepository moderationActionRepository;

    public User requirePostingAllowed(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
        if (!user.isEnabled() || user.isLocked()) {
            throw new AccessDeniedException("Tài khoản đang bị khóa hoặc vô hiệu hóa");
        }
        Instant suspendedUntil = user.getSuspendedUntil();
        if (suspendedUntil != null && suspendedUntil.isAfter(Instant.now())) {
            throw new AccessDeniedException("Tài khoản đang bị tạm đình chỉ đến " + suspendedUntil);
        }
        Instant restrictedUntil = user.getCommunityPostingRestrictedUntil();
        if (restrictedUntil != null && restrictedUntil.isAfter(Instant.now())) {
            throw new AccessDeniedException("Tài khoản đang bị hạn chế đăng bài đến " + restrictedUntil);
        }
        if (moderationActionRepository != null) {
            Page<ModerationAction> activeActions = moderationActionRepository.findAccountActionsByTargetId(
                    userId,
                    List.of(ModerationActionType.RESTRICT, ModerationActionType.SUSPEND),
                    PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "actionAt")));
            for (ModerationAction action : activeActions) {
                if (action.getExpiresAt() != null && action.getExpiresAt().isAfter(Instant.now())) {
                    String actionName = action.getActionType() == ModerationActionType.SUSPEND ? "tạm đình chỉ" : "hạn chế đăng bài";
                    String reasonSuffix = (action.getReason() != null && !action.getReason().isBlank()) ? " do " + action.getReason() : "";
                    throw new AccessDeniedException("Tài khoản đang bị " + actionName + " đến " + action.getExpiresAt() + reasonSuffix);
                }
            }
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
                || ((question.getStatus() == QuestionStatus.AI_PENDING
                        || question.getStatus() == QuestionStatus.PENDING)
                    && question.getAuthorId().equals(userId));
    }

}
