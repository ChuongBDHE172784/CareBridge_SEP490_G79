package com.carebridge.backend.family.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FamilyDashboardResponse(
        List<Group> groups,
        Aggregate globalAggregate,
        UUID selectedCareGroupId,
        Detail selectedGroupDetail) {

    public record Aggregate(long overdue, long dueSoon, long inProgress, long alerts) {
    }

    public record Group(
            UUID id,
            String name,
            Instant joinedAt,
            Instant lastActivityAt,
            String relationshipRole,
            String customRelationshipRole,
            Permission permissionScope,
            Aggregate aggregate) {
    }

    public record TodayReminder(
            UUID id,
            String title,
            String type,
            String status,
            Instant scheduledAt,
            Instant dueAt,
            Instant snoozedUntil,
            int priority) {
    }

    public record Alert(
            UUID id,
            UUID careGroupId,
            String title,
            String body,
            Instant createdAt,
            boolean read) {
    }

    public record Permission(
            boolean calendar,
            boolean logs,
            boolean alerts,
            boolean checklistView,
            boolean records,
            boolean quickNotes,
            boolean quickNoteWeight,
            boolean quickNoteHydration,
            boolean quickNoteEpds,
            boolean quickNoteFetalMovement,
            boolean quickNoteBloodPressure,
            boolean quickNoteBloodGlucose) {
    }

    public record Member(
            UUID memberId,
            UUID userId,
            String displayName,
            String systemRole,
            String relationshipRole,
            String customRelationshipRole,
            Instant joinedAt) {
    }

    public record SharedDataCategory(String category, boolean permitted, int itemCount) {
    }

    public record SharedDataSummary(int totalItems, List<SharedDataCategory> categories) {
    }

    /** Sanitized, read-only projection. Missing observations keep nullable values instead of fake zeroes. */
    public record HealthMetricSummary(
            String metricType,
            BigDecimal valueNumeric,
            BigDecimal valueSecondary,
            String unit,
            Instant measuredAt,
            String measurementContext,
            int recordCount) {
    }

    public record Detail(
            UUID careGroupId,
            String motherDisplayName,
            List<TodayReminder> todayReminders,
            List<Alert> alerts,
            int memberCount,
            List<Member> members,
            String relationshipRole,
            String customRelationshipRole,
            Permission permissionScope,
            SharedDataSummary sharedDataSummary,
            List<HealthMetricSummary> healthMetricSummaries) {
    }
}
