package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.TopicType;
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
public class UpdateCommunityTopicRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "icon must not exceed 255 characters")
    private String icon;

    // null = leave unchanged; a different value is rejected because type is immutable (COM-017).
    private TopicType type;

    // null = leave unchanged. A TOPIC may provide another visible CATEGORY id for reassignment.
    private UUID parentId;

    private Boolean isHidden;

    private Integer sortOrder;
}
