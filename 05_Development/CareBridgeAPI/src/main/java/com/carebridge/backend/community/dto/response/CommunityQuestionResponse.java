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
public class CommunityQuestionResponse {

    private UUID id;
    private UUID topicId;
    private String title;
    private String body;
    private String stage;
    private String urgency;
    private boolean anonymous;
    private Long authorId;
    private String status;
    private Instant createdAt;
}
