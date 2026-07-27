package com.carebridge.backend.notification;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class NotificationCanonicalSchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationStartsWithOnlyTheCanonicalNotificationSchema() {
        String legacyTable = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.notifications')::text",
                String.class);
        String canonicalTable = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.notification_records')::text",
                String.class);
        Integer requiredColumns = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'notification_records'
                  AND column_name IN ('channel', 'updated_at')
                  AND is_nullable = 'NO'
                """, Integer.class);
        var constraints = jdbcTemplate.queryForMap("""
                SELECT
                    max(pg_get_constraintdef(oid)) FILTER (WHERE conname = 'notification_records_type_check') AS type_check,
                    max(pg_get_constraintdef(oid)) FILTER (WHERE conname = 'notification_records_status_check') AS status_check,
                    max(pg_get_constraintdef(oid)) FILTER (WHERE conname = 'notification_records_channel_check') AS channel_check
                FROM pg_constraint
                WHERE conrelid = 'public.notification_records'::regclass
                """);

        assertThat(legacyTable).isNull();
        assertThat(canonicalTable).isEqualTo("notification_records");
        assertThat(requiredColumns).isEqualTo(2);
        assertThat(constraints.get("type_check").toString())
                .contains(
                        "REMINDER",
                        "COMMUNITY_REPLY",
                        "CONSULTATION",
                        "EMERGENCY",
                        "MESSAGE",
                        "GROUP_INVITE",
                        "CONTENT_REVIEW");
        assertThat(constraints.get("status_check").toString())
                .contains("PENDING", "PROCESSING", "SENT", "DELIVERED", "FAILED");
        assertThat(constraints.get("channel_check").toString())
                .contains("PUSH", "EMAIL", "IN_APP");
    }
}
