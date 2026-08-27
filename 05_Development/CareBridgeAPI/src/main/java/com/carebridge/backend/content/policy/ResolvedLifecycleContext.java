package com.carebridge.backend.content.policy;

import com.carebridge.backend.content.entity.ContentStage;
import java.util.UUID;

public record ResolvedLifecycleContext(UUID journeyId, ContentStage stage) {
}
