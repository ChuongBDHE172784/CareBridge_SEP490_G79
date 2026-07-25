package com.carebridge.backend.consultation.context.repository;

import com.carebridge.backend.consultation.context.entity.ConsultationContextShare;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationContextShareRepository
        extends JpaRepository<ConsultationContextShare, UUID> {

    Optional<ConsultationContextShare> findByOwnerUserIdAndIdempotencyKey(
            UUID ownerUserId, UUID idempotencyKey);

    Optional<ConsultationContextShare> findByConsultationRequestId(UUID consultationRequestId);
}
