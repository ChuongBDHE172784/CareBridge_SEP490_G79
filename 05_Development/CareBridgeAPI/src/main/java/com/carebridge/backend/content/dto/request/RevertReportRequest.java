package com.carebridge.backend.content.dto.request;

// CB-MOD-IMP-015: reason is optional — recorded on the new ModerationAction(UNDO) row (if any)
// and in the audit log entry.
public record RevertReportRequest(
        String reason
) {
    public RevertReportRequest() {
        this(null);
    }
}
