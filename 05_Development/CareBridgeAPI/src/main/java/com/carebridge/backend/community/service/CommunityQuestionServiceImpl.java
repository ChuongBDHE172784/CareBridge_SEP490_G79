package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityQuestionServiceImpl implements CommunityQuestionService {

    private final CommunityQuestionRepository questionRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityQuestionMapper questionMapper;
    private final AuditService auditService;

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
}
