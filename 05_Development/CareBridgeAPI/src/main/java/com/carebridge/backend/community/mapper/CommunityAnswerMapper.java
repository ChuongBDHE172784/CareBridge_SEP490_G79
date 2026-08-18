package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

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
 .imageUrls(copyImageUrls(request.getImageUrls()))
 .personalExperience(!expertLabeled && Boolean.TRUE.equals(request.getIsPersonalExperience()))
 .experienceTag(expertLabeled ? null : request.getExperienceTag())
 .expertLabeled(expertLabeled) // ADR-COM-005: never from request
 .status(AnswerStatus.AI_PENDING)
 .build();
}

public CommunityAnswerResponse toResponse(CommunityAnswer entity) {
 return toResponse(entity, null, false, null);
}

public CommunityAnswerResponse toResponse(CommunityAnswer entity, boolean liked) {
 return toResponse(entity, null, liked, null);
}

// UC-59 hydration fix: "liked" reflects the CURRENT viewer's like state, computed by the
// caller (batch per-user lookup) — never derivable from the entity alone.
// expertProfileId: populated when author is a verified expert, enabling navigation from
// community answers to the expert's public profile (TV4 integration).
public CommunityAnswerResponse toResponse(CommunityAnswer entity, String authorDisplay, boolean liked, UUID expertProfileId) {
 String finalAuthorDisplay;
 if (entity.isExpertLabeled()) {
  finalAuthorDisplay = (authorDisplay != null && !authorDisplay.isBlank()) ? authorDisplay : "Chuyên gia";
 } else {
  finalAuthorDisplay = (authorDisplay != null && !authorDisplay.isBlank()) ? authorDisplay : "Thành viên";
 }
 return CommunityAnswerResponse.builder()
 .id(entity.getId())
 .questionId(entity.getQuestionId())
 .authorId(entity.getAuthorId())
 .authorDisplay(finalAuthorDisplay)
 .body(entity.getBody())
 .imageUrls(copyImageUrls(entity.getImageUrls()))
 .personalExperience(entity.isPersonalExperience())
 .experienceTag(entity.getExperienceTag())
 .expertLabeled(entity.isExpertLabeled())
 .expertProfileId(expertProfileId)
 .status(toResponseStatus(entity.getStatus()))
 .likeCount(entity.getLikeCount())
 .liked(liked)
 .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : java.time.Instant.now())
 .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : (entity.getCreatedAt() != null ? entity.getCreatedAt() : java.time.Instant.now()))
 .build();
}

private List<String> copyImageUrls(List<String> imageUrls) {
 return imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
}

private String toResponseStatus(AnswerStatus status) {
 return status == AnswerStatus.AI_PENDING ? AnswerStatus.PENDING.name()
         : (status != null ? status.name() : null);
}

// UC-200: apply partial edit — body and isPersonalExperience only.
// questionId, authorId and expertLabeled are never touched here (ADR-COM-200-1, BR-COM-200-4).
public void applyEdit(CommunityAnswer entity, EditAnswerRequest request) {
 entity.setBody(request.getBody());
 entity.setPersonalExperience(Boolean.TRUE.equals(request.getIsPersonalExperience()));
 entity.setExperienceTag(Boolean.TRUE.equals(request.getIsPersonalExperience()) ? request.getExperienceTag() : null);
 if (request.getImageUrls() != null) {
  entity.setImageUrls(copyImageUrls(request.getImageUrls()));
 }
}
}
