package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Verification document repository.
 */
@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {

    /**
     * Find documents by expert ID.
     *
     * @param expertId the expert ID
     * @return list of documents
     */
    List<VerificationDocument> findByExpertId(Long expertId);

    /**
     * Find documents by expert ID and status.
     *
     * @param expertId the expert ID
     * @param status the document status
     * @return list of documents
     */
    List<VerificationDocument> findByExpertIdAndReviewStatus(Long expertId, VerificationDocument.VerificationDocumentStatus status);
}
