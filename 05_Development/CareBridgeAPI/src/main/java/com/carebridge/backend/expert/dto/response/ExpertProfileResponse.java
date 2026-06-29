package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.entity.ConsultationModality;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfileResponse {

    private UUID id;
    private UUID userId;
    private String displayName;
    private String bio;
    private List<String> specialties;
    private Integer yearsOfExperience;
    private Long consultationFeeVnd;
    private List<ConsultationModality> consultationModalities;
    private ExpertProfileStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
