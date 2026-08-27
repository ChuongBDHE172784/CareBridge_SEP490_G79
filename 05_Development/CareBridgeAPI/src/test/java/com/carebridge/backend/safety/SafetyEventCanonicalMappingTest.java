package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.emergency.entity.EmergencyAlertAttempt;
import com.carebridge.backend.emergency.entity.EmergencyAlertDelivery;
import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.entity.FamilyAlertLog;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
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

    @Test
    void canonicalActionEntitiesPersistSafetyActionDiscriminatorAndZeroAlertDefaults() throws Exception {
        for (Class<?> actionType : new Class<?>[] {
                SafetyEventResponseRecord.class,
                EmergencyAlertAttempt.class,
                EmergencyAlertDelivery.class,
                FamilyAlertLog.class,
                EmergencyMapHandoff.class
        }) {
            assertDefaultField(actionType, "recordType", "SAFETY_ACTION");
            assertDefaultField(actionType, "alertGeneration", 0L);
            assertDefaultField(actionType, "alertSuccessfulRecipientCount", 0);
            assertDefaultField(actionType, "alertFailedRecipientCount", 0);
            assertColumn(actionType, "recordType", "record_type");
            assertColumn(actionType, "alertGeneration", "alert_generation");
            assertColumn(actionType, "alertSuccessfulRecipientCount", "alert_successful_recipient_count");
            assertColumn(actionType, "alertFailedRecipientCount", "alert_failed_recipient_count");
        }
    }

    private static void assertColumn(String fieldName, String columnName) throws Exception {
        assertColumn(SafetyEvent.class, fieldName, columnName);
    }

    private static void assertColumn(
            Class<?> entityType,
            String fieldName,
            String columnName) throws Exception {
        Field field = entityType.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isFalse();
    }

    private static void assertDefaultField(
            Class<?> entityType,
            String fieldName,
            Object expected) throws Exception {
        Object entity = entityType.getDeclaredConstructor().newInstance();
        Field field = entityType.getDeclaredField(fieldName);
        field.setAccessible(true);
        assertThat(field.get(entity)).isEqualTo(expected);
    }
}
