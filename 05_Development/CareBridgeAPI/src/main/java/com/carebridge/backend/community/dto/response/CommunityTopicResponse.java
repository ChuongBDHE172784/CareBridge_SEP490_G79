package com.carebridge.backend.community.dto.response;

import com.carebridge.backend.community.entity.TopicType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTopicResponse {

    private UUID id;
    private String name;
    private String description;
    private String icon;
    private TopicType type;
    private String slug;
    private UUID parentId;

    // Count of APPROVED CommunityQuestion rows under this topic (ADR-COM-015). Defaults to 0 for
    // freshly created/updated single-topic responses that don't go through batch hydration.
    private long questionCount;

    @JsonProperty("isHidden")
    private boolean isHidden;

    @JsonProperty("isFollowed")
    private boolean isFollowed;

    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
