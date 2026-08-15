package com.carebridge.backend.content.dto.response;

import java.util.List;
import java.util.UUID;

public record ChecklistTemplateBatchImportResponse(
        int totalRows,
        int successCount,
        int failedCount,
        List<String> errors,
        List<UUID> createdIds
) {

    public ChecklistTemplateBatchImportResponse {
        errors = List.copyOf(errors);
        createdIds = List.copyOf(createdIds);
    }
}
