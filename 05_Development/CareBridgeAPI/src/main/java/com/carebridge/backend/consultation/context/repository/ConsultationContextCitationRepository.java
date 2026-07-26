package com.carebridge.backend.consultation.context.repository;

import com.carebridge.backend.consultation.context.entity.ConsultationContextCitation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationContextCitationRepository
        extends JpaRepository<ConsultationContextCitation, UUID> {

    List<ConsultationContextCitation> findByContextShareIdOrderByOrdinalAsc(UUID contextShareId);
}
