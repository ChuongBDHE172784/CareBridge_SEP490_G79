package com.carebridge.backend.family.dto;

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
            boolean records,
            boolean quickNotes,
            boolean quickNoteWeight,
            boolean quickNoteHydration,
            boolean quickNoteEpds,
            boolean quickNoteFetalMovement) {
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
            SharedDataSummary sharedDataSummary) {
    }
}
