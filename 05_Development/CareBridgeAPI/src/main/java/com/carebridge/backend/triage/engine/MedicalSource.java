package com.carebridge.backend.triage.engine;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MedicalSource {
    String title;
    String organization;
    String url;
    String lastReviewed;
    String topic;
    String ageRange;
    String body;
}
