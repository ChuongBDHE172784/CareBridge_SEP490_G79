package com.carebridge.backend.canonical;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.location.entity.LocationSnapshot;
import com.carebridge.backend.reminder.entity.CareTask;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.content.entity.ModerationAction;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class CanonicalPhysicalTableMappingTest {

    @Test
    void scheduledCareTaskUsesCanonicalTableAndTimestampColumn() throws Exception {
        assertThat(CareTask.class.getAnnotation(Table.class).name()).isEqualTo("care_tasks");
        assertThat(column(CareTask.class, "dueAt")).isEqualTo("scheduled_at");
        assertThat(CareTask.builder().build().getTaskType()).isEqualTo("SCHEDULED_REMINDER");
    }

    @Test
    void locationSnapshotUsesCanonicalSafetyLedgerColumns() throws Exception {
        assertThat(LocationSnapshot.class.getAnnotation(Table.class).name()).isEqualTo("safety_events");
        assertThat(column(LocationSnapshot.class, "locationSnapshotId")).isEqualTo("safety_event_id");
        assertThat(column(LocationSnapshot.class, "userId")).isEqualTo("user_id");
        assertThat(LocationSnapshot.builder().build().getActionType()).isEqualTo("LOCATION_SNAPSHOT");
    }

    @Test
    void profileProjectionsUseCanonicalUsersTable() throws Exception {
        assertThat(UserProfile.class.getAnnotation(Table.class).name()).isEqualTo("users");
        assertThat(column(UserProfile.class, "profileId")).isEqualTo("user_id");
    }

    @Test
    void moderationActionsUseCanonicalAuditLedger() throws Exception {
        assertThat(ModerationAction.class.getAnnotation(Table.class).name()).isEqualTo("audit_events");
        assertThat(column(ModerationAction.class, "id")).isEqualTo("audit_event_id");
        assertThat(column(ModerationAction.class, "reportId")).isEqualTo("subject_reference_id");
        assertThat(column(ModerationAction.class, "targetId")).isEqualTo("resource_id");
    }

    private String column(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class).name();
    }
}
