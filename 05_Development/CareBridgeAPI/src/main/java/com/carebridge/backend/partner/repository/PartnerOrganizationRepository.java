package com.carebridge.backend.partner.repository;

import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerOrganizationRepository extends JpaRepository<PartnerOrganization, UUID> {

    Optional<PartnerOrganization> findByRepresentativeUserId(java.util.UUID userId);

    boolean existsByRepresentativeUserId(java.util.UUID userId);

    boolean existsByEmail(String email);

    // UC-113: dashboard aggregation — active partner organizations
    long countByStatus(OrganizationStatus status);

    @Query("select p from PartnerOrganization p where (:status is null or p.status = :status) "
            + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')))")
    Page<PartnerOrganization> searchVerificationQueue(
            @Param("status") OrganizationStatus status, @Param("search") String search, Pageable pageable);
}
