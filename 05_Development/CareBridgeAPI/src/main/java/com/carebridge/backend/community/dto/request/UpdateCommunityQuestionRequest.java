package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.UrgencyLevel;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCommunityQuestionRequest {

    @Size(min = 5, max = 255, message = "title must be between 5 and 255 characters")
    private String title;

    @Size(min = 10, max = 5000, message = "body must be between 10 and 5000 characters")
    private String body;

    private Boolean isAnonymous;

    private UrgencyLevel urgency;
}
