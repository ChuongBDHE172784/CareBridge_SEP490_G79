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
                .contains("event.emergencysessionid = :emergencysessionid")
                .contains("event.status = :expectedstatus")
                .contains("event.recordtype = 'imu_event'");
    }

    private static String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
