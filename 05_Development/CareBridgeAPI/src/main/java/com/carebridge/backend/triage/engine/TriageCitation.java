package com.carebridge.backend.triage.engine;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TriageCitation {
    String title;
    String source;
    String url;
    String excerpt;
    String retrievedAt;
}
