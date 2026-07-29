package com.carebridge.backend.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EditAnswerRequest {

    @NotBlank(message = "body is required")
    @Size(min = 10, max = 3000, message = "body must be between 10 and 3000 characters")
    private String body;

    @NotNull(message = "isPersonalExperience is required")
    private Boolean isPersonalExperience;

    @Size(max = 3, message = "imageUrls must contain at most 3 images")
    private List<@NotBlank @Pattern(
            regexp = "^https://res\\.cloudinary\\.com/.+",
            message = "image URL must be hosted by Cloudinary") String> imageUrls;

    // isExpertLabeled is NOT accepted from request — set by Moderator/System only (ADR-COM-005)
}
