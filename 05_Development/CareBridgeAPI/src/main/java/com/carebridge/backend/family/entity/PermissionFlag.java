package com.carebridge.backend.family.entity;

/**
 * Permission flag keys used by CareGroupAccessPolicy.hasPermission().
 * Open — shape owned by sibling UC72_ManageFamilyPermission; keys map to
 * boolean fields in permission_json (ADR-FAM-003).
 */
public enum PermissionFlag {
    CALENDAR,
    LOGS,
    ALERTS
}
