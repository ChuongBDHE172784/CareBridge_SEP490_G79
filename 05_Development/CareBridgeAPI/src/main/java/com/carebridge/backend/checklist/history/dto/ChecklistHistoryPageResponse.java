package com.carebridge.backend.checklist.history.dto;

import java.util.List;

public record ChecklistHistoryPageResponse(
        List<ChecklistHistoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
