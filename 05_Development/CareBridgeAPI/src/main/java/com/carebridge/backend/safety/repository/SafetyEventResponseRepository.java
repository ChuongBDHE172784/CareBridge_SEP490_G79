package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Insert/read-only access to immutable RESPONSE action snapshots. */
@Repository
@RequiredArgsConstructor
public class SafetyEventResponseRepository {

    private final EntityManager entityManager;

    public SafetyEventResponseRecord insert(SafetyEventResponseRecord response) {
        entityManager.persist(response);
        entityManager.flush();
        return response;
    }

    public boolean existsBySafetyEventId(UUID safetyEventId) {
        return entityManager.createQuery("""
                        select count(response) from SafetyEventResponseRecord response
                        where response.safetyEventId = :safetyEventId
                        """, Long.class)
                .setParameter("safetyEventId", safetyEventId)
                .getSingleResult() > 0;
    }
}
