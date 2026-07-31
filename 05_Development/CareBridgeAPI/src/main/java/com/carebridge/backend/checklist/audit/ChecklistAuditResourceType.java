package com.carebridge.backend.checklist.audit;

/** Closed resource vocabulary for checklist audit subjects. */
public enum ChecklistAuditResourceType {
    CHECKLIST_TEMPLATE_VERSION,
    CHECKLIST_INSTANCE,
    CHECKLIST_TASK_INSTANCE,
    CARE_CONTEXT,
    MIGRATION_SOURCE
}
