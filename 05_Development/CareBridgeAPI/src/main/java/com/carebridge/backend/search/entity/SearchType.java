package com.carebridge.backend.search.entity;

/**
 * UC-13 Search and Filter — supported cross-cutting search domains.
 *
 * <p>Logic Issue L3 (Test-Spec §2): descoped from the original TDS §5.1 EXPERT/PROFILE
 * providers — this codebase has no {@code expert} bounded context yet (verified via
 * {@code find src/main/java/.../expert}), and profile display-name search would expose
 * user_profiles data without an established visibility/consent model. Only QUESTION
 * (community) and CONTENT (verified content library) are implemented, both of which
 * already have real, working search queries this feature can delegate to.
 */
public enum SearchType {
    QUESTION,
    CONTENT
}
