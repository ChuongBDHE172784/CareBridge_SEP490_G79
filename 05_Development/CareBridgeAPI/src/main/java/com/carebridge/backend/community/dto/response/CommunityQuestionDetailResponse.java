package com.carebridge.backend.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityQuestionDetailResponse {

    private UUID id;
    private UUID topicId;
    private String topicName;
    private String title;
    private String body;
    private List<String> imageUrls;
    private String stage;
    private Short pregnancyWeek;
    private Short babyAgeMonths;
    private String urgency;
    private boolean anonymous;
    private UUID authorId;
    private String authorDisplay;
    private String status;
    private int answerCount;
    private int likeCount;

    @JsonProperty("isBookmarked")
    private boolean isBookmarked;

    @JsonProperty("isLiked")
    private boolean isLiked;

    private Instant createdAt;
    private Instant updatedAt;
    private List<CommunityAnswerResponse> answers;
}
