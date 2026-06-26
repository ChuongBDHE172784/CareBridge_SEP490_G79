package com.carebridge.backend.community.dto.request;

import jakarta.validation.constraints.Size;
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

    private Boolean isHidden;

    private Integer sortOrder;
}
