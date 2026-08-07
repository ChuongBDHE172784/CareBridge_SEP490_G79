package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Contract test for the database-consolidation feature-retirement waves
 * (V3 §3.1–§3.4, migrations V20260806100000 and V20260806110000).
 *
 * <p>Reaching this test at all is the primary evidence: the base class applies the
 * whole Flyway chain to a real PostgreSQL 18 instance and boots the application
 * with {@code ddl-auto: validate}, so a mapping left pointing at a dropped column
 * fails before any assertion runs. The assertions then pin the two halves of the
 * contract that a green startup alone would not catch — that the retired objects
 * really are gone, and that the deliberately retained ones survived.
 */
class ConsolidationContractEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean relationExists(String name) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + name));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT count(*) > 0
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, Boolean.class, table, column));
    }

    @Test
    void retiredTablesAndViewAreGone() {
        assertThat(List.of(
                "partner_organizations",
                "account_deletion_requests",
                "account_lock_appeals",
                "device_connections",
                "archived_records",
                "nearby_support_interactions",
                // Wave 13 (V20260807160000): growth readings live in health_observations,
                // grouped by measurement_group_id.
                "growth_measurements"))
                .allSatisfy(name -> assertThat(relationExists(name))
                        .as("retired relation %s", name)
                        .isFalse());
    }

    @Test
    void retiredColumnsAreGone() {
        assertThat(columnExists("care_facilities", "partner_id")).isFalse();
        assertThat(columnExists("health_observations", "device_connection_id")).isFalse();
        assertThat(columnExists("direct_conversations", "mother_last_read_at")).isFalse();
        assertThat(columnExists("direct_conversations", "mother_last_read_message_id")).isFalse();
        assertThat(columnExists("direct_conversations", "expert_last_read_at")).isFalse();
        assertThat(columnExists("direct_conversations", "expert_last_read_message_id")).isFalse();
    }

    @Test
    void retainedObjectsSurvived() {
        // plan §4.14 negative scope: these resemble the retired objects closely
        // enough that a careless CASCADE would have taken them too.
        assertThat(List.of(
                "device_tokens",
                "direct_conversation_read_cursors",
                "reminder_occurrence_aliases",
                "audit_events",
                "auth_sessions",
                "care_facilities",
                "health_observations",
                "safety_events"))
                .allSatisfy(name -> assertThat(relationExists(name))
                        .as("retained relation %s", name)
                        .isTrue());

        assertThat(columnExists("users", "settings_jsonb")).isTrue();
    }

    @Test
    void partnerRoleIsRejectedByTheUsersRoleConstraint() {
        String constraint = jdbcTemplate.queryForObject("""
                SELECT pg_get_constraintdef(oid)
                  FROM pg_constraint
                 WHERE conrelid = 'public.users'::regclass AND conname = 'users_role_check'
                """, String.class);

        assertThat(constraint).doesNotContain("PARTNER");
        assertThat(constraint).contains("MOTHER", "FAMILY", "EXPERT", "SYSTEM_ADMIN");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.users WHERE role = 'PARTNER'", Long.class))
                .isZero();
    }

    /**
     * R6 (V3 §3.6). These rules used to be enforced by UNIQUE/NOT NULL constraints
     * on reminder_schedule_times; moving to an array must not quietly lose them.
     */
    @Test
    void reminderLocalTimesArrayKeepsTheRulesTheChildTableEnforced() {
        assertThat(columnExists("reminder_schedules", "local_times")).isTrue();

        // A NULL element means a reminder with no time — unschedulable.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT public.carebridge_validate_reminder_local_times(ARRAY['08:00', NULL]::time[])",
                Boolean.class)).isFalse();

        // A duplicate would materialise two identical jobs for one occurrence.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT public.carebridge_validate_reminder_local_times(ARRAY['08:00','08:00']::time[])",
                Boolean.class)).isFalse();

        // Order is display/execution order, so an unsorted array stays valid.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT public.carebridge_validate_reminder_local_times(ARRAY['21:00','08:00']::time[])",
                Boolean.class)).isTrue();

        // Empty is valid on its own; the active-schedule rule is a separate CHECK.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT public.carebridge_validate_reminder_local_times(ARRAY[]::time[])",
                Boolean.class)).isTrue();
    }

    @Test
    void anActiveReminderScheduleCannotHaveAnEmptyTimeArray() {
        UUID ownerId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM public.users ORDER BY created_at LIMIT 1", UUID.class);
        assertThat(ownerId).as("seeded user to own the schedule").isNotNull();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO public.reminder_schedules
                    (owner_user_id, title, time_zone, start_date, active, local_times)
                VALUES (?, 'Uống vitamin', 'Asia/Ho_Chi_Minh', current_date, true, ARRAY[]::time[])
                """, ownerId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Inactive schedules may be empty, so deactivating never destroys the times.
        assertThat(jdbcTemplate.update("""
                INSERT INTO public.reminder_schedules
                    (owner_user_id, title, time_zone, start_date, active, local_times)
                VALUES (?, 'Đã tắt', 'Asia/Ho_Chi_Minh', current_date, false, ARRAY[]::time[])
                """, ownerId)).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO public.reminder_schedules
                    (owner_user_id, title, time_zone, start_date, active, local_times)
                VALUES (?, 'Trùng giờ', 'Asia/Ho_Chi_Minh', current_date, true,
                        ARRAY['07:30','07:30']::time[])
                """, ownerId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * R7 (V3 §3.7). Schema v1 is closed on purpose: the planner only understands
     * offsetMinutes, so anything else in the JSON would be silently ignored.
     */
    @Test
    void appointmentRulesJsonbEnforcesTheClosedSchema() {
        assertThat(columnExists("appointment_notification_configs", "rules_jsonb")).isTrue();

        assertThat(validRules("[{\"offsetMinutes\": -1440}, {\"offsetMinutes\": -60}]")).isTrue();
        assertThat(validRules("[]")).isTrue();

        // Root must be an array, not a bare object or scalar.
        assertThat(validRules("{\"offsetMinutes\": -60}")).isFalse();
        assertThat(validRules("-60")).isFalse();

        // Range is [-43200, 10080] inclusive.
        assertThat(validRules("[{\"offsetMinutes\": -43200}]")).isTrue();
        assertThat(validRules("[{\"offsetMinutes\": 10080}]")).isTrue();
        assertThat(validRules("[{\"offsetMinutes\": -43201}]")).isFalse();
        assertThat(validRules("[{\"offsetMinutes\": 10081}]")).isFalse();

        // Integer minutes only.
        assertThat(validRules("[{\"offsetMinutes\": -30.5}]")).isFalse();
        assertThat(validRules("[{\"offsetMinutes\": \"-30\"}]")).isFalse();
        assertThat(validRules("[{\"offsetMinutes\": null}]")).isFalse();

        // A duplicate offset would materialise two identical jobs per occurrence.
        assertThat(validRules("[{\"offsetMinutes\": -60}, {\"offsetMinutes\": -60}]")).isFalse();

        // Closed schema: unknown keys are rejected rather than ignored.
        assertThat(validRules("[{\"offsetMinutes\": -60, \"channel\": \"PUSH\"}]")).isFalse();
        assertThat(validRules("[{\"channel\": \"PUSH\"}]")).isFalse();
    }

    private Boolean validRules(String json) {
        return jdbcTemplate.queryForObject(
                "SELECT public.carebridge_validate_appointment_rules(?::jsonb)", Boolean.class, json);
    }

    /**
     * R8 (V3 §3.9). The two CHECKs that guarded safety_configs had to be ported,
     * not reinvented — losing them would let an unauditable consent or an
     * unsupported countdown reach the fall-detection hot path.
     */
    @Test
    void safetyConfigColumnsCarryTheConstraintsPortedFromSafetyConfigs() {
        assertThat(columnExists("users", "fall_detection_enabled")).isTrue();
        assertThat(columnExists("users", "fall_detection_sensitivity_level")).isTrue();
        assertThat(columnExists("users", "emergency_auto_alert")).isTrue();
        assertThat(columnExists("users", "emergency_countdown_seconds")).isTrue();
        assertThat(columnExists("users", "sensor_permission_granted")).isTrue();
        assertThat(columnExists("users", "sensor_permission_recorded_at")).isTrue();
        assertThat(columnExists("users", "safety_config_updated_at")).isTrue();
        assertThat(columnExists("users", "safety_config_updated_by")).isTrue();

        UUID userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM public.users ORDER BY created_at LIMIT 1", UUID.class);

        // Countdown is a closed set, exactly as safety_configs enforced.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE public.users SET emergency_countdown_seconds = 45 WHERE user_id = ?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE public.users SET fall_detection_sensitivity_level = 'EXTREME' WHERE user_id = ?",
                userId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Granting sensor permission without recording when is unauditable consent.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE public.users
                   SET sensor_permission_granted = true, sensor_permission_recorded_at = NULL
                 WHERE user_id = ?
                """, userId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.update("""
                UPDATE public.users
                   SET sensor_permission_granted = true, sensor_permission_recorded_at = now()
                 WHERE user_id = ?
                """, userId)).isEqualTo(1);
    }

    @Test
    void safetyConfigDefaultsMatchTheApplicationDefaults() {
        // plan §5.4: a user who never opened the safety screen must read exactly
        // what the old service used to synthesise.
        var defaults = jdbcTemplate.queryForMap("""
                SELECT column_name, column_default
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'users'
                   AND column_name = 'emergency_countdown_seconds'
                """);
        assertThat(defaults.get("column_default").toString()).contains("30");

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.users
                 WHERE fall_detection_enabled = false
                   AND fall_detection_sensitivity_level = 'MEDIUM'
                   AND emergency_auto_alert = true
                """, Long.class)).isPositive();
    }

    /**
     * R10 (V3 §3.11). The session fields sit on the booking row, so "at most one
     * logical session per booking" stops being a convention and becomes a shape the
     * schema cannot express otherwise.
     */
    @Test
    void consultationSessionFieldsLiveOnTheBookingRow() {
        assertThat(columnExists("consultation_bookings", "communication_room_id")).isTrue();
        assertThat(columnExists("consultation_bookings", "session_started_at")).isTrue();
        assertThat(columnExists("consultation_bookings", "session_ended_at")).isTrue();
        assertThat(columnExists("consultation_bookings", "session_status")).isTrue();
        assertThat(columnExists("consultation_bookings", "expert_summary")).isTrue();
        assertThat(columnExists("consultation_bookings", "technical_log_json")).isTrue();
        assertThat(columnExists("consultation_bookings", "session_created_at")).isTrue();
        assertThat(columnExists("consultation_bookings", "legacy_session_id")).isTrue();

        // The source table is gone as of the persistence contract; the migration's
        // own gate proved every session had reached a booking before it dropped.
        assertThat(relationExists("consultation_sessions")).isFalse();
    }

    @Test
    void consultationBookingRejectsAnImpossibleSessionShape() {
        UUID bookingId = jdbcTemplate.queryForObject(
                "SELECT booking_id FROM public.consultation_bookings ORDER BY created_at LIMIT 1",
                UUID.class);
        assertThat(bookingId).as("seeded consultation booking").isNotNull();

        // Ending before starting is a data error, not a very short consultation.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE public.consultation_bookings
                   SET session_started_at = now(), session_ended_at = now() - interval '1 hour'
                 WHERE booking_id = ?
                """, bookingId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // A completed session with no timestamps cannot be reported on.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE public.consultation_bookings
                   SET session_status = 'COMPLETED',
                       session_started_at = NULL, session_ended_at = NULL
                 WHERE booking_id = ?
                """, bookingId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // An end with no start is equally unreportable.
        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE public.consultation_bookings
                   SET session_started_at = NULL, session_ended_at = now()
                 WHERE booking_id = ?
                """, bookingId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void retiredPaidConsultationFlowIsFullyReconciled() {
        // V3 §3.11 zero-row gate: a written exception does not substitute for it.
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM public.consultation_bookings
                 WHERE expert_price_id IS NOT NULL
                    OR price_band_id IS NOT NULL
                    OR price_snapshot_amount IS NOT NULL
                    OR commission_rate_snapshot IS NOT NULL
                    OR price_locked_at IS NOT NULL
                """, Long.class)).isZero();

        // Cardinality is now structural rather than checked: the session fields sit
        // on the booking row, so a booking cannot carry two sessions and a session
        // cannot exist without one.
        assertThat(relationExists("consultation_sessions")).isFalse();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM (
                    SELECT legacy_session_id FROM public.consultation_bookings
                     WHERE legacy_session_id IS NOT NULL
                     GROUP BY legacy_session_id HAVING count(*) > 1) duplicated
                """, Long.class)).isZero();
    }

    @Test
    void deactivationShapeIsEnforcedOnUsers() {
        // Deactivation replaced the deletion queue, so a DEACTIVATED row that is
        // still enabled or has no timestamp would be an account nobody can account for.
        String constraint = jdbcTemplate.queryForObject("""
                SELECT pg_get_constraintdef(oid)
                  FROM pg_constraint
                 WHERE conrelid = 'public.users'::regclass
                   AND conname = 'users_deactivation_shape_ck'
                """, String.class);

        assertThat(constraint).isNotNull();

        assertThat(columnExists("users", "deactivated_at")).isTrue();
        assertThat(columnExists("users", "deactivation_reason")).isTrue();
        assertThat(columnExists("users", "deactivated_by")).isTrue();
    }
}
