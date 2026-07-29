package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCommunityQuestionRequest {

    private UUID topicId;

    @Size(min = 5, max = 255, message = "title must be between 5 and 255 characters")
    private String title;

    @Size(min = 10, max = 5000, message = "body must be between 10 and 5000 characters")
    private String body;

    @Size(max = 3, message = "imageUrls must contain at most 3 images")
    private List<@NotBlank @Pattern(
            regexp = "^https://res\\.cloudinary\\.com/.+",
            message = "image URL must be hosted by Cloudinary") String> imageUrls;

    private PregnancyStage stage;

    @Min(value = 1, message = "pregnancyWeek must be >= 1")
    @Max(value = 42, message = "pregnancyWeek must be <= 42")
    private Integer pregnancyWeek;

    @Min(value = 0, message = "babyAgeMonths must be >= 0")
    @Max(value = 72, message = "babyAgeMonths must be <= 72")
    private Integer babyAgeMonths;

    private Boolean isAnonymous;

    private UrgencyLevel urgency;
}
