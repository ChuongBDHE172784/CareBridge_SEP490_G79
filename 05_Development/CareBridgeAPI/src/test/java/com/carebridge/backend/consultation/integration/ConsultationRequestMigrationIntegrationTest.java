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
                WHERE table_schema = 'public' AND table_name = 'expert_consultation_requests'
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
                WHERE conrelid = 'public.expert_consultation_requests'::regclass
                """,
                String.class);
        // Canonical schema (V20260727010000 convergence): the direct-conversation FK carries
        // the default PostgreSQL name, not the legacy *_direct_conversation_archive_fk alias.
        assertThat(constraints).contains(
                "expert_consultation_requests_owner_client_uk",
                "expert_consultation_requests_status_ck",
                "expert_consultation_requests_window_ck",
                "expert_consultation_requests_responded_ck",
                "expert_consultation_requests_expiry_ck",
                "expert_consultation_requests_direct_conversation_id_fkey");

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'expert_consultation_requests'
                """,
                String.class);
        assertThat(indexes).contains(
                "expert_consultation_requests_expert_status_ix",
                "expert_consultation_requests_owner_status_ix",
                "expert_consultation_requests_expiry_ix",
                "expert_consultation_requests_integrity_uk");
    }
}
