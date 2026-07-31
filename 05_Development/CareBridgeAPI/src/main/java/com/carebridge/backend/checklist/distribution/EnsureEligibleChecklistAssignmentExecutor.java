package com.carebridge.backend.checklist.distribution;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Spring-proxied, per-candidate transaction boundary for request materialization. */
@Component
public class EnsureEligibleChecklistAssignmentExecutor {

    private final ChecklistDistributionService distributionService;

    public EnsureEligibleChecklistAssignmentExecutor(ChecklistDistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(ChecklistDistributionCommand candidate) {
        distributionService.distributeDetailed(candidate);
    }
}
