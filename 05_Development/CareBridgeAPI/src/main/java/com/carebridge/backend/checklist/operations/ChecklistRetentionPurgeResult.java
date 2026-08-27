package com.carebridge.backend.checklist.operations;

public record ChecklistRetentionPurgeResult(
        long auditEventsPurged,
        long actionCommandsPurged) {
}
