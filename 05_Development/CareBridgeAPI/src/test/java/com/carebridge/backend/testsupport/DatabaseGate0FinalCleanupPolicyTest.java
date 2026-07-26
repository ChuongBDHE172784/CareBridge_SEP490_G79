package com.carebridge.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseGate0FinalCleanupPolicyTest {

    @Test
    void publishesTheApprovedSeventeenAndProtectsBlockedAndReleaseOneTables() {
        var manifest = DatabaseGate0Support.inspectRepository();
        var policy = manifest.finalCleanupPolicy();

        assertThat(policy.approvedRemovalCandidates())
                .containsExactlyInAnyOrderElementsOf(DatabaseGate0Support.REMOVAL_CANDIDATES)
                .hasSize(17);
        assertThat(policy.blockedOrDependentTables())
                .containsExactlyInAnyOrderElementsOf(DatabaseGate0Support.BLOCKED_OR_DEPENDENT_TABLES)
                .hasSize(9);
        assertThat(policy.retainedRelease1Guards())
                .containsExactlyInAnyOrderElementsOf(DatabaseGate0Support.RETAINED_RELEASE1_GUARDS)
                .contains("nearby_support_requests", "nearby_support_responses", "care_facilities",
                        "health_summaries", "audit_logs", "consent_grants");

        assertThat(DatabaseGate0Support.REMOVAL_CANDIDATES)
                .doesNotContainAnyElementsOf(DatabaseGate0Support.BLOCKED_OR_DEPENDENT_TABLES)
                .doesNotContainAnyElementsOf(DatabaseGate0Support.RETAINED_RELEASE1_GUARDS);
        assertThat(DatabaseGate0Support.REMOVAL_CANDIDATES)
                .doesNotContain("notifications", "roles", "user_roles", "triage_answers",
                        "triage_assessments", "safety_alerts", "safety_events",
                        "safety_monitoring_settings", "emergency_events", "hospitals");
    }

    @Test
    void recordsOnlyTheThreeKnownCleanBootstrapAbsencesAndExpectedCounts() {
        var policy = DatabaseGate0Support.inspectRepository().finalCleanupPolicy();

        assertThat(policy.knownCleanBootstrapAbsentCandidates())
                .containsExactlyInAnyOrder(
                        "contribution_attachments",
                        "expert_identity_verifications",
                        "medical_contributions");
        assertThat(policy.expectedPublicTableCounts())
                .containsEntry("repositoryBeforeFinalCleanup", 113)
                .containsEntry("repositoryAfterFinalCleanup", 99)
                .containsEntry("liveAuditBaseline", 127)
                .containsEntry("liveAfterBatch1To5", 121)
                .containsEntry("liveAfterFinalCleanup", 104);

        int repositoryDropCount = DatabaseGate0Support.REMOVAL_CANDIDATES.size()
                - DatabaseGate0Support.KNOWN_CLEAN_BOOTSTRAP_ABSENT_CANDIDATES.size();
        assertThat(repositoryDropCount).isEqualTo(14);
        assertThat(113 - repositoryDropCount).isEqualTo(99);
        assertThat(121 - DatabaseGate0Support.REMOVAL_CANDIDATES.size()).isEqualTo(104);
    }

    @Test
    void mapsEveryCandidateToItsExactSuccessfulRemovalMigration() {
        assertThat(DatabaseGate0Support.CANDIDATE_REMOVAL_VERSIONS)
                .containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                        Map.entry("commission_config", "20260722020800"),
                        Map.entry("commission_records", "20260722020800"),
                        Map.entry("consultation_disputes", "20260722020800"),
                        Map.entry("consultation_messages", "20260722020800"),
                        Map.entry("consultation_requests", "20260722020800"),
                        Map.entry("expert_reviews", "20260722020800"),
                        Map.entry("payment_transactions", "20260722020800"),
                        Map.entry("refund_records", "20260722020800"),
                        Map.entry("settlement_records", "20260722020800"),
                        Map.entry("partner_expert_links", "20260722020900"),
                        Map.entry("partner_services", "20260722020900"),
                        Map.entry("sponsored_campaigns", "20260722020900"),
                        Map.entry("contribution_attachments", "20260722021000"),
                        Map.entry("expert_identity_verifications", "20260722021000"),
                        Map.entry("expert_verification_documents", "20260722021000"),
                        Map.entry("impact_assessment_ratings", "20260722021000"),
                        Map.entry("medical_contributions", "20260722021000")));
    }
}
