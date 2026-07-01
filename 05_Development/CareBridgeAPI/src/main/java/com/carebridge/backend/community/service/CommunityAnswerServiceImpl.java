package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.AnswerNotEditableException;
import com.carebridge.backend.community.exception.AnswerNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityAnswerServiceImpl implements CommunityAnswerService {

    private final CommunityAnswerRepository answerRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerMapper answerMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public CommunityAnswerResponse postAnswer(UUID authorId, UUID questionId, PostCommunityAnswerRequest request) {
        // ADR-COM-006: only APPROVED questions accept answers
        questionRepository.findByIdAndStatus(questionId, QuestionStatus.APPROVED)
                .orElseThrow(() -> new QuestionNotAnswerableException(questionId.toString()));

        CommunityAnswer answer = answerMapper.toEntity(request, authorId, questionId);
        answer = answerRepository.save(answer);

        auditService.log(AuditAction.COMMUNITY_ANSWER_POSTED, authorId,
                "CommunityAnswer", answer.getId().toString(), "posted");

        return answerMapper.toResponse(answer);
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

        answerMapper.applyEdit(answer, request);
        // ADR-COM-200-2: reset to PENDING so edited content is re-moderated
        answer.setStatus(AnswerStatus.PENDING);

        answer = answerRepository.save(answer);
        auditService.log(AuditAction.COMMUNITY_ANSWER_EDITED, callerId,
                "CommunityAnswer", answer.getId().toString(), "edited");

        return answerMapper.toResponse(answer);
    }

    @Override
    @Transactional
    public void deleteAnswer(UUID answerId, UUID callerId, boolean isModeratorCaller) {
        CommunityAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerNotFoundException(answerId.toString()));

        if (!answer.getAuthorId().equals(callerId) && !isModeratorCaller) {
            throw new AccessDeniedException("You do not own this answer");
        }

        // ADR-COM-201-3: only decrement answer_count if the previous status was APPROVED
        boolean wasApproved = answer.getStatus() == AnswerStatus.APPROVED;

        answer.setStatus(AnswerStatus.DELETED);
        answerRepository.save(answer);

        if (wasApproved) {
            questionRepository.decrementAnswerCount(answer.getQuestionId());
        }

        auditService.log(AuditAction.COMMUNITY_ANSWER_DELETED, callerId,
                "CommunityAnswer", answerId.toString(), "deleted");
    }
}
