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

    // Required iff type=TOPIC and must reference an existing visible CATEGORY (ADR-COM-020).
    // CATEGORY/TAG must leave this null; the service performs the cross-row validation.
    private UUID parentId;

    // Boxed (not primitive int): with @AllArgsConstructor present, Jackson can pass null for an
    // absent JSON field, which throws for primitives ("Cannot map null into type int"). null here
    // means "not provided" -> mapper defaults it to 0, same convention as UpdateCommunityTopicRequest.
    @Min(value = 0, message = "sortOrder must be non-negative")
    private Integer sortOrder;
}
