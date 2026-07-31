package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.ObservationShape;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricCapabilityResponse {

    private String metricCode;
    private int version;
    private String displayName;
    private ObservationShape observationShape;
    private String subjectType;
    private boolean manualEntrySupported;
    private boolean deviceImportSupported;
    private String canonicalUnit;
    private List<String> acceptedInputUnits;
    private Short precisionScale;
    private Map<String, Object> requiredContextSchema;
    private Map<String, Object> qualityPolicy;
    private List<String> allowedJourneyStages;
}
