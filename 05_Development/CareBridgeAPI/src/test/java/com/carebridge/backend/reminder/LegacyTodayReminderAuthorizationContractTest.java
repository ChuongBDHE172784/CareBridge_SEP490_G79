package com.carebridge.backend.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegacyTodayReminderAuthorizationContractTest {

    @Test
    void legacyTodayReusesHardenedActorScopedReminderProjection() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/reminder/service/impl/TodayTaskServiceImpl.java"));

        assertThat(service)
                .contains("ReminderAccessPolicy")
                .contains("findActiveOwnerUserIdsForChecklistViewer(callerId)")
                .contains("accessPolicy.canView")
                .doesNotContain("findByUserIdAndInviteStatus(callerId, InviteStatus.ACCEPTED)");
    }
}
