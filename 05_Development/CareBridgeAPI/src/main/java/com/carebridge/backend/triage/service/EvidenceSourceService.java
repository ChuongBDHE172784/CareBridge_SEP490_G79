package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.entity.EvidenceSourceReviewLog;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface EvidenceSourceService {
    EvidenceSource propose(String baseUrl, String organization, String category, String applicableStages, String notes, UUID actorUserId);
    List<EvidenceSource> list(String status);
    List<EvidenceSource> approvedForStage(String stage);
    EvidenceSource changeStatus(UUID id, String newStatus, String notes, UUID actorUserId, String actorRole);
    List<EvidenceSourceReviewLog> reviewLog(UUID id);
    boolean isApprovedDeepLink(URI uri);
}
