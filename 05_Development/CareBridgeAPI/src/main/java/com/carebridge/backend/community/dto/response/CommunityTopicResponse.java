package com.carebridge.backend.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTopicResponse {

    private UUID id;
    private String name;
    private String description;
    private String icon;

    @JsonProperty("isHidden")
    private boolean isHidden;

    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
