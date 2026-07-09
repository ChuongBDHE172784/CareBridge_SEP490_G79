package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CommunityAnswerMapper {

    public CommunityAnswer toEntity(PostCommunityAnswerRequest request, UUID authorId, UUID questionId) {
        return toEntity(request, authorId, questionId, false);
    }

    public CommunityAnswer toEntity(PostCommunityAnswerRequest request, UUID authorId, UUID questionId,
            boolean expertLabeled) {
        return CommunityAnswer.builder()
                .questionId(questionId)
                .authorId(authorId)
                .body(request.getBody())
                .personalExperience(Boolean.TRUE.equals(request.getIsPersonalExperience()))
                .expertLabeled(expertLabeled)   // ADR-COM-005: never from request
                .status(AnswerStatus.PENDING) // ADR-COM-006: always PENDING
                .build();
    }

    public CommunityAnswerResponse toResponse(CommunityAnswer entity) {
        return toResponse(entity, null, false);
    }

    public CommunityAnswerResponse toResponse(CommunityAnswer entity, boolean liked) {
        return toResponse(entity, null, liked);
    }

    // UC-59 hydration fix: "liked" reflects the CURRENT viewer's like state, computed by the
    // caller (batch per-user lookup) — never derivable from the entity alone.
    public CommunityAnswerResponse toResponse(CommunityAnswer entity, String authorDisplay, boolean liked) {
        String finalAuthorDisplay;
        if (entity.isExpertLabeled()) {
            finalAuthorDisplay = "Chuyên gia";
        } else {
            finalAuthorDisplay = (authorDisplay != null && !authorDisplay.isBlank()) ? authorDisplay : "Thành viên";
        }
        return CommunityAnswerResponse.builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .authorId(entity.getAuthorId())
                .authorDisplay(finalAuthorDisplay)
                .body(entity.getBody())
                .personalExperience(entity.isPersonalExperience())
                .expertLabeled(entity.isExpertLabeled())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .likeCount(entity.getLikeCount())
                .liked(liked)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // UC-200: apply partial edit — body and isPersonalExperience only.
    // questionId, authorId and expertLabeled are never touched here (ADR-COM-200-1, BR-COM-200-4).
    public void applyEdit(CommunityAnswer entity, EditAnswerRequest request) {
        entity.setBody(request.getBody());
        entity.setPersonalExperience(Boolean.TRUE.equals(request.getIsPersonalExperience()));
    }
}
