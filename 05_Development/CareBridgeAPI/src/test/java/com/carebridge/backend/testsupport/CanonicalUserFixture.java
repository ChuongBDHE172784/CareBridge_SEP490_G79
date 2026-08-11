package com.carebridge.backend.testsupport;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test-only creator for canonical users rows. The legacy persons table is gone:
 * users carries the profile columns directly and users.person_id stores the
 * owner's own user_id (the value care_subjects.person_id points at).
 */
public final class CanonicalUserFixture {

    private CanonicalUserFixture() {
    }

    public static void insertUser(
            JdbcTemplate jdbcTemplate,
            UUID userId,
            String displayName,
            String phone,
            String role) {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, person_id, full_name, display_name, phone, "
                        + "role, enabled, locked, email_verified, phone_verified, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, true, false, false, false, now(), now())",
                userId, userId, displayName, displayName, phone, role);
    }
}
