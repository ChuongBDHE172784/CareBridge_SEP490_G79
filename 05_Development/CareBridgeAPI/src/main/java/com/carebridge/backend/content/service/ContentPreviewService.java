package com.carebridge.backend.content.service;

import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.Map;
import java.util.UUID;

public interface ContentPreviewService {

    String fetchPreview(UUID targetId, ReportTargetType targetType);

    Map<UUID, String> batchFetchPreviews(Iterable<UUID> targetIds, ReportTargetType targetType);
}
