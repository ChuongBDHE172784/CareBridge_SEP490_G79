package com.carebridge.backend.checklist.sequence;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChecklistSequenceAdvanceRequest(
        @NotNull UUID currentInstanceId,
        @NotNull UUID clientRequestId) {
}
