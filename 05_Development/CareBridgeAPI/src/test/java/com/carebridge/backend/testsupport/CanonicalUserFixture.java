package com.carebridge.backend.testsupport;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Test-only creator for the canonical persons -> users ownership boundary. */
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
                "INSERT INTO persons (person_id, display_name, phone_number, created_at, updated_at) "
                        + "VALUES (?, ?, ?, now(), now())",
                userId, displayName, phone);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, person_id, full_name, phone, role, enabled, locked, "
                        + "email_verified, phone_verified, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, true, false, false, false, now(), now())",
                userId, userId, displayName, phone, role);
    }
}
