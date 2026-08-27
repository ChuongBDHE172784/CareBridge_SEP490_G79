package com.carebridge.backend.checklist.today.dto;

import java.util.List;

public record TodayTaskSections(
        List<TodayTaskItemResponse> overdue,
        List<TodayTaskItemResponse> today,
        List<TodayTaskItemResponse> upcoming,
        List<TodayTaskItemResponse> unscheduled) {
}
