package com.carebridge.backend.partner.repository;

import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerOrganizationRepository extends JpaRepository<PartnerOrganization, UUID> {

    Optional<PartnerOrganization> findByRepresentativeUserId(java.util.UUID userId);

    boolean existsByRepresentativeUserId(java.util.UUID userId);

    boolean existsByEmail(String email);

    // UC-113: dashboard aggregation — active partner organizations
    long countByStatus(OrganizationStatus status);
}
