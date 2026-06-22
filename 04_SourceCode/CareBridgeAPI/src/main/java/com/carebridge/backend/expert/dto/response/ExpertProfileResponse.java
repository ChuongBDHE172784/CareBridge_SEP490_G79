package com.carebridge.backend.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfileResponse {

    private UUID id;

    private UUID userId;

    private String bio;

    private List<String> expertiseAreas;

    private Integer yearsExperience;

    private String qualifications;

    private BigDecimal hourlyRate;

    private BigDecimal avgRating;

    private Integer totalReviews;

    private Boolean isVerified;

    private Boolean isAvailable;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
}
