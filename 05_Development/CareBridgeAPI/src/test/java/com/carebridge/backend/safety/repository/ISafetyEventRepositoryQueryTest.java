package com.carebridge.backend.safety.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.safety.SafetyEventStatus;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

class ISafetyEventRepositoryQueryTest {

    @Test
    void alertSentTransitionIsAtomicIdempotentAndRestrictedToImuEvents() throws Exception {
        Method method = ISafetyEventRepository.class.getMethod(
                "transitionAlertSentByEmergencySessionId",
                UUID.class,
                SafetyEventStatus.class,
                SafetyEventStatus.class);

        Modifying modifying = method.getAnnotation(Modifying.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(modifying).isNotNull();
        assertThat(query).isNotNull();
        assertThat(normalize(query.value()))
                .contains("event.emergency_session_id = :emergencysessionid")
                .contains("event.status = :#{#expectedstatus.name()}")
                .contains("record_type = 'imu_event'")
                .contains("updated_at = now()");
    }

    @Test
    void reusedSentEmergencyTransitionChecksCanonicalSessionState() throws Exception {
        Method method = ISafetyEventRepository.class.getMethod(
                "transitionLinkedEventForSentEmergencySession",
                UUID.class,
                SafetyEventStatus.class,
                SafetyEventStatus.class);

        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(normalize(query.value()))
                .contains("session.record_type = 'emergency_session'")
                .contains("session.status = 'active'")
                .contains("session.alert_status = 'sent'")
                .contains("event.emergency_session_id = session.safety_event_id")
                .contains("updated_at = now()");
    }

    private static String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
