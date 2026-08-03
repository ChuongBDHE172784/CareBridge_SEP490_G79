package com.carebridge.backend.checklist.today.dto;

import java.util.List;

public record CurrentChecklistSections(
        List<CurrentChecklistTaskResponse> overdue,
        List<CurrentChecklistTaskResponse> today,
        List<CurrentChecklistTaskResponse> upcoming,
        List<CurrentChecklistTaskResponse> unscheduled) {
}
