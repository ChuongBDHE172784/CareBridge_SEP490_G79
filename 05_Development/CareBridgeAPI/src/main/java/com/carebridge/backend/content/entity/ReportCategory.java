package com.carebridge.backend.content.entity;

/**
 * UC-14 Report Content or Account (CB-MOD-IMP-014 ADR-001). Values must match the
 * {@code reason_code} on {@code moderation_cases}, migrated from the legacy category value.
 * Stored as a plain String on {@link ContentReport#category}
 * (not {@code @Enumerated}) to avoid breaking existing String-typed consumers
 * ({@code ModerationMapper.getCategory()}, {@code ResolveReportServiceImplTest} fixtures) —
 * this enum only exists at the API/DTO boundary for type-safety on the create-report request.
 */
public enum ReportCategory {
    INACCURATE_INFORMATION,
    DISGUISED_ADVERTISING,
    HARASSMENT,
    UNSAFE_ADVICE,
    SPAM,
    OTHER
}
