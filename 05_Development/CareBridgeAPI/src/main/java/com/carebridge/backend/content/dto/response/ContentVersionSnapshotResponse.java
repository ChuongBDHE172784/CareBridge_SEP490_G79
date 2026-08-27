package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record ContentVersionSnapshotResponse(Integer versionNo, String title, String stage, String status,
                                             String sourceSummary, List<UUID> tagIds,
                                             Short eligibleFromWeek, Short eligibleToWeek,
                                             Short recommendationPriority, UUID changedBy, Instant createdAt) {}
