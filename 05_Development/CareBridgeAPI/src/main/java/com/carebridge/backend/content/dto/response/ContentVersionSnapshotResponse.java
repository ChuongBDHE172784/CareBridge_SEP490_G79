package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ContentVersionSnapshotResponse(Integer versionNo, String title, String stage, String status,
                                             String sourceSummary, UUID changedBy, Instant createdAt) {}
