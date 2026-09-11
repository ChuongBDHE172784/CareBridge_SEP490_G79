package com.carebridge.backend.testsupport;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test-only creator for canonical users rows. The legacy persons table is gone:
 * users carries the profile columns directly and users.person_id stores the
 * owner's own user_id (the value care_subjects.person_id points at).
 */
public final class CanonicalUserFixture {

    /**
     * users.phone is globally unique (users_phone_canonical_uk) and every Postgres
     * integration test shares one container for the whole JVM, so seeded phones must
     * be unique across test classes, not just within one.
     *
     * <p>Several fixtures built the number from
     * {@code String.valueOf(System.nanoTime()).substring(0, 8)}, which takes the
     * leading digits — the high-order ones, which barely move between calls — so
     * consecutive users were handed the same phone and the insert collided. The
     * counter below cannot repeat within a JVM; the random start keeps it clear of
     * phone numbers that tests hard-code.
     */
    private static final AtomicLong PHONE_SEQUENCE =
            new AtomicLong(ThreadLocalRandom.current().nextLong(10_000_000L, 90_000_000L));

    private CanonicalUserFixture() {
    }

    /** Returns {@code prefix} followed by 8 digits that are unique within this JVM. */
    public static String uniquePhone(String prefix) {
        return prefix + String.format("%08d", PHONE_SEQUENCE.incrementAndGet() % 100_000_000L);
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
