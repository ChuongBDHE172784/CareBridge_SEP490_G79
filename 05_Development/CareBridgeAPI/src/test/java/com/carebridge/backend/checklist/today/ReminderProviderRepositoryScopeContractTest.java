package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderProviderRepositoryScopeContractTest {

    @Test
    void familyReminderDiscoveryUsesActorScopedAuthorizedOwnerQueryInsteadOfAllActiveGroups()
            throws Exception {
        String repository = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/family/repository/CareGroupRepository.java"));
        String provider = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/checklist/today/provider/ReminderTodayTaskProvider.java"));

        assertThat(repository)
                .contains("findActiveOwnerUserIdsForChecklistViewer")
                .contains("CHECKLIST_VIEW")
                .contains("invitation_status = 'ACCEPTED'");
        assertThat(provider)
                .contains("findActiveOwnerUserIdsForChecklistViewer(actorUserId)")
                .doesNotContain("findByStatus(CareGroupStatus.ACTIVE)");
    }
}
