package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Insert/read-only access to the immutable MAP_HANDOFF action journal. */
@Repository
@RequiredArgsConstructor
public class EmergencyMapHandoffRepository {

    private final EntityManager entityManager;

    public EmergencyMapHandoff insert(EmergencyMapHandoff handoff) {
        entityManager.persist(handoff);
        entityManager.flush();
        return handoff;
    }

    public Optional<EmergencyMapHandoff> findById(UUID handoffId) {
        return Optional.ofNullable(entityManager.find(EmergencyMapHandoff.class, handoffId));
    }

    public List<EmergencyMapHandoff> findByUserId(UUID userId) {
        return entityManager.createQuery("""
                        select handoff from EmergencyMapHandoff handoff
                        where handoff.userId = :userId
                        order by handoff.createdAt desc
                        """, EmergencyMapHandoff.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<EmergencyMapHandoff> findByStatus(HandoffStatus status) {
        return entityManager.createQuery("""
                        select handoff from EmergencyMapHandoff handoff
                        where handoff.status = :status
                        order by handoff.createdAt
                        """, EmergencyMapHandoff.class)
                .setParameter("status", status)
                .getResultList();
    }
}
