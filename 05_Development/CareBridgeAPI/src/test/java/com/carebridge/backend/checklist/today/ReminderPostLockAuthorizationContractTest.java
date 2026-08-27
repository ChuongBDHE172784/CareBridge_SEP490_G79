package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReminderPostLockAuthorizationContractTest {

    @Test
    void lockedReminderAuthorizationRefreshesManagedStateBeforePermissionCheck() throws Exception {
        String handler = Files.readString(Path.of(
                "src/main/java/com/carebridge/backend/checklist/today/provider/"
                        + "ReminderTaskActionHandler.java"));

        assertThat(handler)
                .contains("findStatusByIdForUpdate")
                .containsAnyOf("entityManager.clear()", "entityManager.refresh(",
                        "session.clear()", "session.refresh(");
    }
}
