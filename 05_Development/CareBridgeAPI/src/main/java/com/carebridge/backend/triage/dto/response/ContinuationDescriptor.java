package com.carebridge.backend.triage.dto.response;

import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.OriginDashboard;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContinuationDescriptor {
    private UUID intakeSessionId;
    private String status;
    private String riskLevel;
    private String stage;
    private UUID journeyId;
    private OriginDashboard originDashboard;
    private UUID originReferenceId;
    private OriginAction originAction;
}
