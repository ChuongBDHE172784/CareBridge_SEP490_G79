package com.carebridge.backend.expert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.service.impl.ExpertProfileServiceImpl;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.masterdata.repository.HospitalRepository;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ExpertProfileServiceImplDirectoryTest {

    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private AuditService auditService;

    private ExpertProfileServiceImpl service;
    private final ExpertProfileMapper mapper = new ExpertProfileMapper();

    private static final UUID EXPERT_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExpertProfileServiceImpl(expertProfileRepository, userRepository, mapper,
                identityRepository, credentialRepository, auditService,
                specialtyRepository, hospitalRepository);
    }

    private static ExpertProfile approvedExpert(UUID userId) {
        return ExpertProfile.builder()
                .expertProfileId(UUID.randomUUID())
                .userId(userId)
                .specialty("Sản khoa")
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
    }

    // MEDI-TC-001 — displayName + avatarUrl resolved from users, not silently null
    @Test
    void getPublicDirectory_returnsDisplayNameAndAvatarFromUsersTable() {
        ExpertProfile expert = approvedExpert(EXPERT_USER_ID);
        Page<ExpertProfile> page = new PageImpl<>(List.of(expert), PageRequest.of(0, 10), 1);
        when(expertProfileRepository.searchDirectory(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(expertProfileRepository.findApprovedSpecialties()).thenReturn(List.of("Nhi khoa", "Sản khoa"));
        User user = User.builder().id(EXPERT_USER_ID).name("Nguyễn Văn A").avatarUrl("https://x/a.jpg").build();
        when(userRepository.findAllById(anySet())).thenReturn(List.of(user));

        ExpertDirectoryResponse response = service.getPublicDirectory(null, null, 0, 10);

        assertThat(response.getExperts()).hasSize(1);
        assertThat(response.getExperts().get(0).getDisplayName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getExperts().get(0).getAvatarUrl()).isEqualTo("https://x/a.jpg");
        assertThat(response.getSpecialties()).containsExactly("Nhi khoa", "Sản khoa");
    }

    // MEDI-TC-003 — page/size actually applied via Pageable, not ignored
    @Test
    void getPublicDirectory_appliesPageAndSize() {
        ExpertProfile expert = approvedExpert(EXPERT_USER_ID);
        Page<ExpertProfile> page = new PageImpl<>(List.of(expert), PageRequest.of(1, 2), 3);
        when(expertProfileRepository.searchDirectory(isNull(), isNull(), eq(PageRequest.of(1, 2)))).thenReturn(page);
        when(expertProfileRepository.findApprovedSpecialties()).thenReturn(List.of("Sản khoa"));
        when(userRepository.findAllById(anySet())).thenReturn(List.of());

        ExpertDirectoryResponse response = service.getPublicDirectory(null, null, 1, 2);

        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(3);
    }
}
