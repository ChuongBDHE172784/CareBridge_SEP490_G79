package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;

import java.util.List;
import java.util.UUID;

public interface HealthMemoryService {
    List<HealthMemoryEntry> list(UUID userId, TriageStage stage, UUID profileId);
    void delete(UUID userId, UUID entryId);
}
