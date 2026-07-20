package com.carebridge.backend.baby.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class BabyLinkRejectionAuditService {
    private final AuditService auditService;
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(UUID actor, UUID opaqueTarget, String reason) {
        auditService.log(AuditAction.BABY_JOURNEY_LINK_REJECTED, actor, "BabyJourneyLink", opaqueTarget == null ? null : opaqueTarget.toString(), Map.of("reason", reason, "source", "API"));
    }
}
