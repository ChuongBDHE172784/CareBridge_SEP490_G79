package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.AnswerNotEditableException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityAnswerServiceImpl implements CommunityAnswerService {

    private final CommunityAnswerRepository answerRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerMapper answerMapper;
    private final AuditService auditService;
    private final CommunitySafetyPolicy communitySafetyPolicy;
    private final CommunityAuthorDisplayResolver authorDisplayResolver;
    private final ExpertProfileRepository expertProfileRepository;

    @Override
    @Transactional
    public CommunityAnswerResponse postAnswer(UUID authorId, UUID questionId, PostCommunityAnswerRequest request) {
        User author = communitySafetyPolicy.requirePostingAllowed(authorId);

        questionRepository.findByIdAndStatus(questionId, QuestionStatus.APPROVED)
            .orElseThrow(() -> new QuestionNotAnswerableException(questionId.toString()));

        boolean expertLabeled = communitySafetyPolicy.isVerifiedActiveExpert(author);
        CommunityAnswer answer = answerMapper.toEntity(request, authorId, questionId, expertLabeled);
        answer = answerRepository.save(answer);
        communitySafetyPolicy.autoReportIfRedFlag(authorId, answer.getId(), ReportTargetType.ANSWER, answer.getBody());

        auditService.log(AuditAction.COMMUNITY_ANSWER_POSTED, authorId,
            "CommunityAnswer", answer.getId().toString(), "posted expertLabeled=" + expertLabeled);

        String displayName = authorDisplayResolver.resolve(authorId);
        UUID expertProfileId = resolveExpertProfileId(authorId);
        return answerMapper.toResponse(answer, displayName, false, expertProfileId);
    }

    @Override
    @Transactional
    public CommunityAnswerResponse editAnswer(UUID answerId, UUID callerId, EditAnswerRequest request) {
        CommunityAnswer answer = answerRepository.findById(answerId)
            .orElseThrow(() -> new AnswerNotFoundException(answerId.toString()));

        if (!answer.getAuthorId().equals(callerId)) {
            throw new AccessDeniedException("Only the author can edit this answer");
        }

        if (answer.getStatus() == AnswerStatus.HIDDEN || answer.getStatus() == AnswerStatus.DELETED) {
            throw new AnswerNotEditableException(answerId.toString());
        }

        boolean wasApproved = answer.getStatus() == AnswerStatus.APPROVED;

        answerMapper.applyEdit(answer, request);
        answer.setStatus(AnswerStatus.PENDING);

        answer = answerRepository.save(answer);
        if (wasApproved) {
            questionRepository.decrementAnswerCount(answer.getQuestionId());
        }
        communitySafetyPolicy.autoReportIfRedFlag(callerId, answer.getId(), ReportTargetType.ANSWER, answer.getBody());
        auditService.log(AuditAction.COMMUNITY_ANSWER_EDITED, callerId,
            "CommunityAnswer", answer.getId().toString(), "edited");

        String displayName = authorDisplayResolver.resolve(callerId);
        UUID expertProfileId = resolveExpertProfileId(callerId);
        return answerMapper.toResponse(answer, displayName, false, expertProfileId);
    }

    @Override
    @Transactional
    public void deleteAnswer(UUID answerId, UUID callerId, boolean isModeratorCaller) {
        CommunityAnswer answer = answerRepository.findById(answerId)
            .orElseThrow(() -> new AnswerNotFoundException(answerId.toString()));

        if (!answer.getAuthorId().equals(callerId) && !isModeratorCaller) {
            throw new AccessDeniedException("You do not own this answer");
        }

        boolean wasDeleted = answer.getStatus() == AnswerStatus.DELETED;
        boolean wasApproved = answer.getStatus() == AnswerStatus.APPROVED;

        answer.setStatus(AnswerStatus.DELETED);
        answerRepository.save(answer);

        if (wasApproved) {
            questionRepository.decrementAnswerCount(answer.getQuestionId());
        }

        if (!wasDeleted) {
            auditService.log(AuditAction.COMMUNITY_ANSWER_DELETED, callerId,
                "CommunityAnswer", answerId.toString(), "deleted");
        }
    }

    private UUID resolveExpertProfileId(UUID userId) {
        Optional<ExpertProfile> ep = expertProfileRepository.findByUserId(userId);
        return ep.filter(p -> p.getVerificationStatus() == VerificationStatus.APPROVED)
                 .map(ExpertProfile::getExpertProfileId)
                 .orElse(null);
    }
}
