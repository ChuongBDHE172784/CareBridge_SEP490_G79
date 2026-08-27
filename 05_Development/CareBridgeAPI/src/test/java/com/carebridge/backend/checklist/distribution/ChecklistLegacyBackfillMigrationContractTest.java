package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChecklistLegacyBackfillMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/test/resources/db/migration-legacy/V20260729070000__backfill_legacy_checklist_v2.sql");

    @Test
    void migrationBackfillsLegacyRowsAndQuarantinesAmbiguousContexts() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql).contains("from public.preparation_checklist_items");
        assertThat(sql).contains("insert into public.checklist_instances");
        assertThat(sql).contains("insert into public.checklist_task_instances");
        assertThat(sql).contains("checklist_migration_quarantine");
        assertThat(sql).contains("in_progress").contains("skipped").contains("unscheduled");
        assertThat(sql).contains("baby_id").contains("mother_journey_id");
    }

    @Test
    void migrationUsesTheRuntimeV1LengthPrefixedSha256KeyAlgorithmAndGoldenVector() throws Exception {
        UUID templateVersionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID recipientUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID careGroupId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID contextId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        String golden = "fad7bba6cefeb717acaf887b59410cef7184b88706e67cdf828be0240678369d";

        assertThat(ChecklistDistributionKeyFactory.instanceKey(
                templateVersionId,
                recipientUserId,
                "MOTHER",
                careGroupId,
                "JOURNEY",
                contextId,
                "NONE",
                "NONE"))
                .isEqualTo(golden);

        String sql = Files.readString(MIGRATION).toLowerCase();
        assertThat(sql)
                .contains("checklist_v1_key")
                .contains("octet_length(convert_to(token, 'utf8'))")
                .contains("encode(sha256(convert_to(")
                .contains(golden)
                .doesNotContain("md5(");
    }

    @Test
    void migrationGroupsOneParentPerCanonicalOccurrenceAndOrdersItsChildren() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql)
                .contains("legacy_parent_group_key")
                .contains("occurrence_start_token")
                .contains("occurrence_end_token")
                .contains("partition by legacy_parent_group_key")
                .contains("order by display_order, source_id")
                .contains("parent_instance_id")
                .contains("checklist_v1_key(")
                .doesNotContain("source_id,\n    source_id,");
    }

    @Test
    void migrationQuarantinesDestinationPayloadDriftAndKeyCollisionsInsteadOfDiscardingThem() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql)
                .contains("legacy_parent_payload_drift")
                .contains("legacy_task_payload_drift")
                .contains("legacy_distribution_key_collision")
                .contains("legacy_task_key_collision")
                .contains("is distinct from")
                .doesNotContain("do nothing");
    }

    @Test
    void everyQuarantineResultWritesTypedCorrelatedRedactedAuditMetadata() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();

        assertThat(sql)
                .contains("insert into public.audit_events")
                .contains("event_category")
                .contains("actor_type")
                .contains("actor_service")
                .contains("reason_code")
                .contains("correlation_id")
                .contains("checklist_migration_quarantined")
                .contains("checklist_legacy_backfill")
                .contains("returning source_id, reason_code, correlation_id")
                .contains("jsonb_build_object")
                .doesNotContain("title_snapshot', title")
                .doesNotContain("'title', title");
    }
}
