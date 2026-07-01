package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotEditableException;
import com.carebridge.backend.community.exception.QuestionLockedException;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityQuestionServiceImpl implements CommunityQuestionService {

    private final CommunityQuestionRepository questionRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityAnswerRepository answerRepository;
    private final CommunityQuestionMapper questionMapper;
    private final CommunityAnswerMapper answerMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public CommunityQuestionDetailResponse getQuestionDetail(UUID questionId) {
        CommunityQuestion question = questionRepository.findById(questionId)
                .filter(q -> q.getStatus() == QuestionStatus.APPROVED)
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

        String topicName = topicRepository.findById(question.getTopicId())
                .map(t -> t.getName())
                .orElse("");

        List<CommunityAnswerResponse> answers = answerRepository
                .findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, AnswerStatus.APPROVED)
                .stream()
                .map(answerMapper::toResponse)
                .toList();

        return questionMapper.toDetailResponse(question, topicName, answers);
    }

    @Override
    @Transactional
    public CommunityQuestionResponse createQuestion(UUID authorId, CreateCommunityQuestionRequest request) {
        // BR-COM-002: reject hidden or non-existent topics (ADR-COM-005)
        topicRepository.findByIdAndIsHiddenFalse(request.getTopicId())
                .orElseThrow(() -> new CommunityTopicNotFoundException(request.getTopicId().toString()));

        CommunityQuestion question = questionMapper.toEntity(request, authorId);
        question = questionRepository.save(question);

        auditService.log(AuditAction.COMMUNITY_QUESTION_CREATED, authorId,
                "CommunityQuestion", question.getId().toString(), "created");

        return questionMapper.toResponse(question);
    }

    @Override
    @Transactional
    public CommunityQuestionResponse editQuestion(UUID authorId, UUID questionId, UpdateCommunityQuestionRequest request) {
        CommunityQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

        if (!question.getAuthorId().equals(authorId)) {
            throw new AccessDeniedException("Only the author can edit this question");
        }

        if (question.getStatus() == QuestionStatus.LOCKED || question.getStatus() == QuestionStatus.HIDDEN) {
            throw new QuestionNotEditableException(questionId.toString());
        }

        if (request.getTitle() != null) question.setTitle(request.getTitle());
        if (request.getBody() != null) question.setBody(request.getBody());
        if (request.getIsAnonymous() != null) question.setAnonymous(request.getIsAnonymous());
        if (request.getUrgency() != null) question.setUrgency(request.getUrgency());

        question = questionRepository.save(question);
        auditService.log(AuditAction.COMMUNITY_QUESTION_EDITED, authorId,
                "CommunityQuestion", question.getId().toString(), "edited");

        return questionMapper.toResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId, UUID callerId, boolean isModeratorCaller) {
        CommunityQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

        if (question.getStatus() == QuestionStatus.LOCKED) {
            throw new QuestionLockedException(questionId.toString());
        }

        if (!question.getAuthorId().equals(callerId) && !isModeratorCaller) {
            throw new AccessDeniedException("You do not own this question");
        }

        question.setStatus(QuestionStatus.DELETED);
        questionRepository.save(question);

        auditService.log(AuditAction.COMMUNITY_QUESTION_DELETED, callerId,
                "CommunityQuestion", questionId.toString(), "deleted");
    }
}
