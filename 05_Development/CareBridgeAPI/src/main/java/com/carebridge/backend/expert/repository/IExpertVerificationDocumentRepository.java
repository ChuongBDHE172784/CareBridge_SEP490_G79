package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertVerificationDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IExpertVerificationDocumentRepository extends JpaRepository<ExpertVerificationDocument, UUID> {

  List<ExpertVerificationDocument> findByExpertId(UUID expertId);

  long countByExpertId(UUID expertId);
}
