package com.carebridge.backend.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfilePublicResponse {

    private UUID id;

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
}
