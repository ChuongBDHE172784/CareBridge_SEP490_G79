package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Table;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/** Contract for the final three-table checklist persistence surface. */
class ChecklistDistributionExtendedDomainContractTest {

    @Test
    void retainedEntitiesMapToExactlyTheThreeRuntimeTables() throws Exception {
        List<String> mappings = List.of(
                "ChecklistInstance:checklist_instances",
                "ChecklistTaskInstance:checklist_task_instances",
                "ChecklistActionCommand:checklist_action_commands");

        for (String mapping : mappings) {
            String[] parts = mapping.split(":");
            Class<?> type = Class.forName("com.carebridge.backend.checklist.entity." + parts[0]);
            assertThat(type.getAnnotation(Table.class).name()).isEqualTo(parts[1]);
        }
        for (String retired : List.of(
                "ChecklistSubstage", "ChecklistCareGroupContext",
                "ChecklistDistributionOutbox", "ChecklistReconciliationRun",
                "ChecklistReconciliationCandidate", "ChecklistMigrationQuarantine")) {
            assertThatThrownBy(() -> Class.forName(
                    "com.carebridge.backend.checklist.entity." + retired))
                    .as(retired)
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void contentAndFamilyContractsExposeV2Fields() throws Exception {
        Class<?> template = Class.forName("com.carebridge.backend.content.entity.ChecklistTemplate");
        Class<?> item = Class.forName("com.carebridge.backend.content.entity.ChecklistItem");
        Class<?> permission = Class.forName("com.carebridge.backend.family.entity.PermissionFlag");

        assertThat(template.getDeclaredField("templateLineageId")).isNotNull();
        assertThat(template.getDeclaredField("templateVersionId")).isNotNull();
        assertThat(template.getDeclaredField("substageId")).isNotNull();
        assertThat(template.getDeclaredField("migrationReviewRequired")).isNotNull();
        assertThat(template.getDeclaredField("distributionEnabled")).isNotNull();
        assertThat(item.getDeclaredField("targetSubject")).isNotNull();
        assertThat(permission.getEnumConstants()).extracting(Object::toString)
                .contains("CHECKLIST_VIEW", "CHECKLIST_COMPLETE");
    }

    @Test
    void actionLedgerCarriesRequiredKeyAndRetentionFields() throws Exception {
        Class<?> command = Class.forName("com.carebridge.backend.checklist.entity.ChecklistActionCommand");

        assertThat(command.getDeclaredField("clientRequestId")).isNotNull();
        assertThat(command.getDeclaredField("payloadHash")).isNotNull();
        assertThat(command.getDeclaredField("retainUntil")).isNotNull();
    }

    @Test
    void onlyRetainedAggregatesRequireRepositoryBoundaries() throws Exception {
        List<String> repositories = List.of(
                "ChecklistInstanceRepository",
                "ChecklistTaskInstanceRepository",
                "ChecklistActionCommandRepository");

        for (String repository : repositories) {
            Class<?> type = Class.forName("com.carebridge.backend.checklist.repository." + repository);
            assertThat(JpaRepository.class).isAssignableFrom(type);
        }
        for (String retired : List.of(
                "ChecklistSubstageRepository", "ChecklistCareGroupContextRepository",
                "ChecklistDistributionOutboxRepository", "ChecklistReconciliationRunRepository",
                "ChecklistReconciliationCandidateRepository", "ChecklistMigrationQuarantineRepository")) {
            assertThatThrownBy(() -> Class.forName(
                    "com.carebridge.backend.checklist.repository." + retired))
                    .as(retired)
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }
}
