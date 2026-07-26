package com.carebridge.backend.expert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ProfessionalSpecialty;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.repository.ProfessionalSpecialtyRepository;
import com.carebridge.backend.expert.service.impl.ExpertProfileServiceImpl;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.masterdata.entity.Specialty;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpertProfileServiceImplSpecialtyTest {

    @Mock private ExpertProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private AuditService auditService;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private CareFacilityRepository facilityRepository;
    @Mock private ProfessionalSpecialtyRepository professionalSpecialtyRepository;

    private ExpertProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExpertProfileServiceImpl(profileRepository, userRepository,
            new ExpertProfileMapper(), identityRepository, credentialRepository, auditService,
            specialtyRepository, facilityRepository, professionalSpecialtyRepository);
    }

    @Test
    void createSynchronizesCanonicalSpecialtiesAndKeepsPrimaryNameProjection() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID primaryId = UUID.randomUUID();
        UUID secondaryId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Specialty primary = specialty(primaryId, "OBGYN", "Sản khoa");
        Specialty secondary = specialty(secondaryId, "PEDIATRICS", "Nhi khoa");
        CareFacility facility = CareFacility.builder().facilityId(facilityId).name("Bệnh viện A").build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(specialtyRepository.findByIdentifier("OBGYN")).thenReturn(Optional.of(primary));
        when(specialtyRepository.findByIdentifier("PEDIATRICS")).thenReturn(Optional.of(secondary));
        when(facilityRepository.findByFacilityIdAndActiveTrue(facilityId))
            .thenReturn(Optional.of(facility));
        when(profileRepository.save(any(ExpertProfile.class))).thenAnswer(invocation -> {
            ExpertProfile profile = invocation.getArgument(0);
            profile.setExpertProfileId(profileId);
            return profile;
        });

        var response = service.createProfile(userId, CreateExpertProfileRequest.builder()
            .specialtyId("OBGYN")
            .specialtyIds(List.of("PEDIATRICS", "OBGYN"))
            .hospitalId(facilityId.toString())
            .build());

        assertThat(response.getSpecialty()).isEqualTo("Sản khoa");
        assertThat(response.getSpecialtyId()).isEqualTo(primaryId.toString());
        ArgumentCaptor<List<ProfessionalSpecialty>> mappings = ArgumentCaptor.forClass(List.class);
        verify(professionalSpecialtyRepository).deleteByProfessionalProfileId(profileId);
        verify(professionalSpecialtyRepository).saveAll(mappings.capture());
        assertThat(mappings.getValue())
            .extracting(ProfessionalSpecialty::getSpecialtyId)
            .containsExactly(primaryId, secondaryId);
        assertThat(mappings.getValue())
            .extracting(ProfessionalSpecialty::isPrimary)
            .containsExactly(true, false);
    }

    @Test
    void verificationStatusUsesLatestProfileDecisionReasonNotCredentialReason() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ExpertProfile profile = ExpertProfile.builder()
            .expertProfileId(profileId)
            .userId(userId)
            .verificationStatus(com.carebridge.backend.expert.verificationstatus.VerificationStatus.REJECTED)
            .build();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.findLatestProfileRejectionReason(profileId))
            .thenReturn(Optional.of("Ảnh hồ sơ không rõ"));

        var status = service.getMyVerificationStatus(userId);

        assertThat(status.getRejectionReason()).isEqualTo("Ảnh hồ sơ không rõ");
    }

    private static Specialty specialty(UUID id, String code, String name) {
        return Specialty.builder()
            .specialtyId(id)
            .code(code)
            .name(name)
            .isActive(true)
            .build();
    }
}
