package com.carebridge.backend.contribution.repository;

import com.carebridge.backend.contribution.entity.ContributionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionAttachmentRepository extends JpaRepository<ContributionAttachment, UUID> {

    List<ContributionAttachment> findByContributionIdOrderByDisplayOrderAsc(UUID contributionId);

    void deleteByContributionId(UUID contributionId);

    List<ContributionAttachment> findByFileId(UUID fileId);

    @Query("DELETE FROM ContributionAttachment a WHERE a.contributionId = :contributionId")
    void deleteByContributionIdCustom(@Param("contributionId") UUID contributionId);
}