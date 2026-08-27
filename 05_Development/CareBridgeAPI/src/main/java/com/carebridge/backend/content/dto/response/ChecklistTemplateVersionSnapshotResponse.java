package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChecklistTemplateVersionSnapshotResponse(Integer versionNo, String name, String stage, String status,
                                                       int itemCount, UUID changedBy, Instant createdAt) {}
