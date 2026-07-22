package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.TopicType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunityTopicRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "icon must not exceed 255 characters")
    private String icon;

    @NotNull(message = "type is required")
    private TopicType type;

    // Required iff type != TOPIC, must reference an existing, visible TOPIC (ADR-COM-016) —
    // validated in CommunityTopicServiceImpl, not here (needs a DB lookup).
    private UUID parentId;

    @Min(value = 0, message = "sortOrder must be non-negative")
    @Builder.Default
    private int sortOrder = 0;
}
