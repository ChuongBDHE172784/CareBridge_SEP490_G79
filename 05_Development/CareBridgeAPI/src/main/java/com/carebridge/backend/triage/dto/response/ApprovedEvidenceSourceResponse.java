package com.carebridge.backend.triage.dto.response;

import com.carebridge.backend.triage.entity.EvidenceSource;
import lombok.Builder;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
@Builder
public class ApprovedEvidenceSourceResponse {
    String id;
    String domain;
    String baseUrl;
    String organization;
    List<String> applicableStages;

    public static ApprovedEvidenceSourceResponse from(EvidenceSource source) {
        List<String> stages = Arrays.stream(source.getApplicableStages().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return builder()
                .id(source.getId().toString())
                .domain(source.getDomain())
                .baseUrl(source.getBaseUrl())
                .organization(source.getOrganization())
                .applicableStages(stages)
                .build();
    }
}
