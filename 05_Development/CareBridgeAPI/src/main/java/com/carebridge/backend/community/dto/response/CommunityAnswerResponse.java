package com.carebridge.backend.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityAnswerResponse {

 private UUID id;
 private UUID questionId;
 private UUID authorId;
 private String authorDisplay;
 private String body;
 private List<String> imageUrls;
 private boolean personalExperience;
 private String experienceTag;
 private boolean expertLabeled;
 private UUID expertProfileId;
 private String status;
 private int likeCount;
 private boolean liked;
 private Instant createdAt;
 private Instant updatedAt;
}
