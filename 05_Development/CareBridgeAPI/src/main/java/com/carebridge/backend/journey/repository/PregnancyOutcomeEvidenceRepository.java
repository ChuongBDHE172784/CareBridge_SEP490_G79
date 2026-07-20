package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PregnancyOutcomeEvidenceRepository
        extends JpaRepository<PregnancyOutcomeEvidence, UUID> {

    Optional<PregnancyOutcomeEvidence> findByJourneyIdAndSubmissionId(
            UUID journeyId, UUID submissionId);

    Optional<PregnancyOutcomeEvidence> findFirstByJourneyIdOrderByRevisionNumberDesc(
            UUID journeyId);
}
