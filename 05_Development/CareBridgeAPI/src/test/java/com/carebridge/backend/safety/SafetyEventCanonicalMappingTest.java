package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.safety.entity.SafetyEvent;
import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class SafetyEventCanonicalMappingTest {

    @Test
    void imuEventsSupplyRequiredCanonicalAlertDefaults() throws Exception {
        SafetyEvent event = SafetyEvent.builder().build();

        assertThat(event.getAlertGeneration()).isZero();
        assertThat(event.getAlertSuccessfulRecipientCount()).isZero();
        assertThat(event.getAlertFailedRecipientCount()).isZero();
        assertColumn("alertGeneration", "alert_generation");
        assertColumn("alertSuccessfulRecipientCount", "alert_successful_recipient_count");
        assertColumn("alertFailedRecipientCount", "alert_failed_recipient_count");
    }

    private static void assertColumn(String fieldName, String columnName) throws Exception {
        Field field = SafetyEvent.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isFalse();
    }
}
