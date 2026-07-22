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

    // null = leave unchanged (PATCH semantics — matches isHidden/sortOrder below)
    private TopicType type;

    // null = leave unchanged. If `type` is being set to TOPIC in the same request, this MUST be
    // null too (a TOPIC can never have a parent) — validated in CommunityTopicServiceImpl.
    private UUID parentId;

    private Boolean isHidden;

    private Integer sortOrder;
}
