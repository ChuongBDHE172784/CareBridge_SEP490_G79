package com.carebridge.backend.consultation.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ConsultationRequestMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private IZegoCloudService zegoCloudService;

    @Test
    void consultationRequestSchema_hasRequiredColumnsConstraintsAndIndexes() {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'consultation_requests'
                ORDER BY ordinal_position
                """,
                String.class);

        assertThat(columns).containsExactly(
                "id",
                "requester_user_id",
                "expert_profile_id",
                "client_request_id",
                "topic",
                "description",
                "preferred_window_start",
                "preferred_window_end",
                "status",
                "reject_reason",
                "direct_conversation_id",
                "responded_at",
                "responded_by",
                "expires_at",
                "created_at",
                "updated_at");

        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'public.consultation_requests'::regclass
                """,
                String.class);
        assertThat(constraints).contains(
                "consultation_requests_client_request_id_key",
                "chk_consultation_requests_status",
                "chk_consultation_requests_window",
                "chk_consultation_requests_responded_fields",
                "chk_consultation_requests_expires_after_created");

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename IN ('consultation_requests', 'notification_records')
                """,
                String.class);
        assertThat(indexes).contains(
                "idx_consultation_requests_expert_status_created",
                "idx_consultation_requests_requester_status_created",
                "idx_consultation_requests_expiry",
                "uq_notification_records_consultation_request");
    }
}
