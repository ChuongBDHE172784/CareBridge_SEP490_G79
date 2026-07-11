package com.carebridge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.partner.dto.request.UpdatePartnerProfileRequest;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.partner.service.PartnerProfileService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UpdatePartnerProfileIntegrationTest extends AbstractPostgresIntegrationTest {
    @Autowired PartnerProfileService service;
    @Autowired PartnerOrganizationRepository partnerRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User owner;
    private PartnerOrganization partner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder().email("partner.update." + System.nanoTime() + "@test.com")
                .role(Role.PARTNER).passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true).locked(false).emailVerified(true).phoneVerified(false).accountStatus("ACTIVE").build());
        partner = partnerRepository.saveAndFlush(PartnerOrganization.builder().name("Old Clinic")
                .type(OrganizationType.CLINIC).address("Old").city("Hanoi").phone("0901234567")
                .email("old." + System.nanoTime() + "@clinic.vn").description("Old")
                .status(OrganizationStatus.APPROVED).representativeUserId(owner.getId()).build());
    }

    private UpdatePartnerProfileRequest request() {
        return UpdatePartnerProfileRequest.builder().name("Updated Clinic").type(OrganizationType.CLINIC)
                .address("New").city("Hanoi").phone("0907654321")
                .email("new." + System.nanoTime() + "@clinic.vn").description("Updated").build();
    }

    @Test
    void pupTcInt001_updatePersistsFieldsAndPreservesStatus() {
        service.updateProfile(request(), owner.getId());
        partnerRepository.flush();
        PartnerOrganization persisted = partnerRepository.findById(partner.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Updated Clinic");
        assertThat(persisted.getDescription()).isEqualTo("Updated");
        assertThat(persisted.getStatus()).isEqualTo(OrganizationStatus.APPROVED);
    }

    @Test
    void pupTcInt002_representativeUserIdRemainsUnchanged() {
        service.updateProfile(request(), owner.getId());
        partnerRepository.flush();
        assertThat(partnerRepository.findById(partner.getId()).orElseThrow().getRepresentativeUserId())
                .isEqualTo(owner.getId());
    }
}
