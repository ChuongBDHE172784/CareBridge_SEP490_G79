package com.carebridge.backend.contribution.repository;

import com.carebridge.backend.contribution.entity.MedicalContribution;
import com.carebridge.backend.contribution.entity.ContributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalContributionRepository extends JpaRepository<MedicalContribution, UUID> {

    Page<MedicalContribution> findByExpertUserId(UUID expertUserId, Pageable pageable);

    Page<MedicalContribution> findByStatus(ContributionStatus status, Pageable pageable);

    List<MedicalContribution> findByExpertUserIdAndStatusIn(UUID expertUserId, List<ContributionStatus> statuses);
}