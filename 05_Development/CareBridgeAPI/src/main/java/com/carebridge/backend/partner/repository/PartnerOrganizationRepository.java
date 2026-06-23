package com.carebridge.backend.partner.repository;

import com.carebridge.backend.partner.entity.PartnerOrganization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerOrganizationRepository extends JpaRepository<PartnerOrganization, UUID> {

    Optional<PartnerOrganization> findByRepresentativeUserId(Long userId);

    boolean existsByRepresentativeUserId(Long userId);

    boolean existsByEmail(String email);
}
