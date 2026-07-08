package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommunityQuestionRequest {

    @NotNull(message = "topicId is required")
    private UUID topicId;

    @NotBlank(message = "title is required")
    @Size(min = 5, max = 255, message = "title must be between 5 and 255 characters")
    private String title;

    @NotBlank(message = "body is required")
    @Size(min = 10, max = 5000, message = "body must be between 10 and 5000 characters")
    private String body;

    @NotNull(message = "stage is required")
    private PregnancyStage stage;

    @Min(value = 1, message = "pregnancyWeek must be >= 1")
    @Max(value = 42, message = "pregnancyWeek must be <= 42")
    private Integer pregnancyWeek;

    @Min(value = 0, message = "babyAgeMonths must be >= 0")
    @Max(value = 72, message = "babyAgeMonths must be <= 72")
    private Integer babyAgeMonths;

    @NotNull(message = "urgency is required")
    private UrgencyLevel urgency;

    @NotNull(message = "isAnonymous is required")
    private Boolean isAnonymous;
}
