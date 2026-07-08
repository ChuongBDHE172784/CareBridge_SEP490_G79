package com.carebridge.backend.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityAnswerResponse {

    private UUID id;
    private UUID questionId;
    private UUID authorId;
    private String body;
    private boolean personalExperience;
    private boolean expertLabeled;
    private String status;
    private int likeCount;
    private boolean liked;
    private Instant createdAt;
    private Instant updatedAt;
}
