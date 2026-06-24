package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CommunityAnswerMapper {

    public CommunityAnswer toEntity(PostCommunityAnswerRequest request, Long authorId, UUID questionId) {
        return CommunityAnswer.builder()
                .questionId(questionId)
                .authorId(authorId)
                .body(request.getBody())
                .personalExperience(Boolean.TRUE.equals(request.getIsPersonalExperience()))
                .expertLabeled(false)   // ADR-COM-005: never from request
                .status(AnswerStatus.PENDING) // ADR-COM-006: always PENDING
                .build();
    }

    public CommunityAnswerResponse toResponse(CommunityAnswer entity) {
        return CommunityAnswerResponse.builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .authorId(entity.getAuthorId())
                .body(entity.getBody())
                .personalExperience(entity.isPersonalExperience())
                .expertLabeled(entity.isExpertLabeled())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
