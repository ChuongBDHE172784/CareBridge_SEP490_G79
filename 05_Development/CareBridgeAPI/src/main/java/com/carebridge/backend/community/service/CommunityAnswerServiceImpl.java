package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import lombok.RequiredArgsConstructor;
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
    public CommunityAnswerResponse postAnswer(Long authorId, UUID questionId, PostCommunityAnswerRequest request) {
        // ADR-COM-006: only APPROVED questions accept answers
        questionRepository.findByIdAndStatus(questionId, QuestionStatus.APPROVED)
                .orElseThrow(() -> new QuestionNotAnswerableException(questionId.toString()));

        CommunityAnswer answer = answerMapper.toEntity(request, authorId, questionId);
        answer = answerRepository.save(answer);

        auditService.log(AuditAction.COMMUNITY_ANSWER_POSTED, authorId,
                "CommunityAnswer", answer.getId().toString(), "posted");

        return answerMapper.toResponse(answer);
    }
}
