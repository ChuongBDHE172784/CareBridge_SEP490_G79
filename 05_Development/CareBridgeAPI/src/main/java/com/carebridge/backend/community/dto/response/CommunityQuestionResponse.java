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
public class CommunityQuestionResponse {

    private UUID id;
    private UUID topicId;
    private String title;
    private String body;
    private List<String> imageUrls;
    private String stage;
    private String urgency;
    private boolean anonymous;
    private UUID authorId;
    private String status;
    private int answerCount;
    private int likeCount;
    private Instant createdAt;
    private Instant updatedAt;
}
