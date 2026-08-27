package com.carebridge.backend.checklist.distribution;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface ChecklistReconciliationSource {
    List<ChecklistDistributionCommand> loadCandidatesForActor(
            UUID actorUserId, LocalDate effectiveDate, ZoneId timezone, UUID correlationId);

}
