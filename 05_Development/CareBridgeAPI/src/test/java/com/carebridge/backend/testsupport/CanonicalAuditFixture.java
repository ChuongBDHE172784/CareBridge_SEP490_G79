package com.carebridge.backend.testsupport;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Controlled cleanup for tests that reuse fixed actor ids against immutable audit_events. */
public final class CanonicalAuditFixture {

    private CanonicalAuditFixture() {
    }

    public static void deleteByActor(JdbcTemplate jdbcTemplate, UUID actorUserId) {
        jdbcTemplate.execute(
                "ALTER TABLE public.audit_events DISABLE TRIGGER audit_events_immutable_trg");
        try {
            jdbcTemplate.update(
                    "DELETE FROM public.audit_events WHERE actor_user_id = ?",
                    actorUserId);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE public.audit_events ENABLE TRIGGER audit_events_immutable_trg");
        }
    }
}
