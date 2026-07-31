package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/** RED contract for the Phase 1 persistence model before implementation classes exist. */
class ChecklistDistributionDomainContractTest {

    @Test
    void checklistInstanceAndTaskEntitiesExposeSeparateParentAndChildTables() throws Exception {
        Class<?> instance = Class.forName("com.carebridge.backend.checklist.entity.ChecklistInstance");
        Class<?> task = Class.forName("com.carebridge.backend.checklist.entity.ChecklistTaskInstance");

        assertThat(instance.getAnnotation(Table.class).name()).isEqualTo("checklist_instances");
        assertThat(task.getAnnotation(Table.class).name()).isEqualTo("checklist_task_instances");
        assertThat(instance.getDeclaredField("distributionKey")).isNotNull();
        assertThat(instance.getDeclaredField("careContextType")).isNotNull();
        assertThat(instance.getDeclaredField("careContextId")).isNotNull();
        assertThat(instance.getDeclaredField("status")).isNotNull();
        assertThat(task.getDeclaredField("taskKey")).isNotNull();
        assertThat(task.getDeclaredField("targetSubject")).isNotNull();
        assertThat(task.getDeclaredField("status")).isNotNull();
    }

    @Test
    void childTargetIsNotStoredAsParentField() throws Exception {
        Class<?> instance = Class.forName("com.carebridge.backend.checklist.entity.ChecklistInstance");

        assertThat(instance.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("targetSubject");
    }
}
