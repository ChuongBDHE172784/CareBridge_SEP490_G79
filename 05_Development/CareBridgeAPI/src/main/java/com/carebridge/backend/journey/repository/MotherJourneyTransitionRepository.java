package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.MotherJourneyTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MotherJourneyTransitionRepository
        extends Repository<MotherJourneyTransition, UUID> {

    <S extends MotherJourneyTransition> S saveAndFlush(S transition);

    Page<MotherJourneyTransition> findByJourneyIdOrderByRecordedAtDesc(
            UUID journeyId, Pageable pageable);

    long countByJourneyId(UUID journeyId);

    long count();
}
