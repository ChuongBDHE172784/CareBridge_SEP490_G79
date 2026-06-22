package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UploadCredentialRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.policy.ExpertProfilePolicy;
import com.carebridge.backend.expert.repository.ExpertCredentialRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertServiceTests {

    @Mock
    private ExpertProfileRepository expertProfileRepository;

    @Mock
    private ExpertCredentialRepository expertCredentialRepository;

    @Mock
    private ExpertProfileMapper expertProfileMapper;

    @Mock
    private ExpertProfilePolicy expertProfilePolicy;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExpertServiceImpl expertService;

    @Captor
    private ArgumentCaptor<UUID> userIdCaptor;

    private UUID testUserId;
    private UUID testProfileId;
    private CreateExpertProfileRequest createRequest;
    private UpdateExpertProfileRequest updateRequest;
    private ExpertProfile testProfile;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProfileId = UUID.randomUUID();

        createRequest = CreateExpertProfileRequest.builder()
                .expertiseAreas(List.of("Cardiology", "Pediatrics"))
                .yearsExperience(10)
                .bio("Experienced cardiologist")
                .qualifications("MD, PhD")
                .hourlyRate(new BigDecimal("150.00"))
                .build();

        updateRequest = UpdateExpertProfileRequest.builder()
                .bio("Updated bio")
                .hourlyRate(new BigDecimal("200.00"))
                .build();

        testProfile = ExpertProfile.builder()
                .id(testProfileId)
                .userId(testUserId)
                .expertiseAreas(List.of("Cardiology"))
                .yearsExperience(5)
                .bio("Original bio")
                .qualifications("MD")
                .hourlyRate(new BigDecimal("100.00"))
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .isVerified(false)
                .isAvailable(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createProfile_success() {
        // Given
        when(expertProfileRepository.existsByUserId(testUserId)).thenReturn(false);
        when(expertProfileRepository.save(any(ExpertProfile.class))).thenAnswer(invocation -> {
            ExpertProfile p = invocation.getArgument(0);
            p.setId(testProfileId);
            return p;
        });
        when(expertProfileMapper.toResponse(any(ExpertProfile.class))).thenReturn(
                ExpertProfileResponse.builder()
                        .id(testProfileId)
                        .userId(testUserId)
                        .build()
        );

        // When
        ExpertProfileResponse response = expertService.createProfile(testUserId, createRequest);

        // Then
        assertNotNull(response);
        assertEquals(testProfileId, response.getId());
        verify(expertProfileRepository).existsByUserId(testUserId);
        verify(expertProfileRepository).save(userIdCaptor.capture());
        verify(auditService).log(any(), eq(testUserId), eq("expert_profile"), any(), isNull());
    }

    @Test
    void createProfile_duplicate_throws() {
        // Given
        when(expertProfileRepository.existsByUserId(testUserId)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> expertService.createProfile(testUserId, createRequest)
        );
        assertEquals("Expert profile already exists for this user", exception.getMessage());
        verify(expertProfileRepository, never()).save(any());
    }

    @Test
    void getOwnProfile_success() {
        // Given
        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(expertProfileMapper.toResponse(testProfile)).thenReturn(
                ExpertProfileResponse.builder()
                        .id(testProfileId)
                        .userId(testUserId)
                        .build()
        );

        // When
        ExpertProfileResponse response = expertService.getOwnProfile(testUserId);

        // Then
        assertNotNull(response);
        assertEquals(testProfileId, response.getId());
    }

    @Test
    void getOwnProfile_notFound_throws() {
        // Given
        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> expertService.getOwnProfile(testUserId)
        );
        assertEquals("Expert profile not found", exception.getMessage());
    }

    @Test
    void updateProfile_success() {
        // Given
        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(expertProfileRepository.save(any(ExpertProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expertProfileMapper.toResponse(any(ExpertProfile.class))).thenReturn(
                ExpertProfileResponse.builder()
                        .id(testProfileId)
                        .userId(testUserId)
                        .bio("Updated bio")
                        .build()
        );

        // When
        ExpertProfileResponse response = expertService.updateProfile(testUserId, updateRequest);

        // Then
        assertNotNull(response);
        assertEquals("Updated bio", response.getBio());
        verify(expertProfilePolicy).checkCanEditProfile(testUserId, testProfileId);
        verify(auditService).log(any(), eq(testUserId), eq("expert_profile"), eq(testProfileId.toString()), isNull());
    }

    @Test
    void updateProfile_notOwner_throws() {
        // Given
        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        doThrow(new org.springframework.security.access.AccessDeniedException("Cannot edit others' profile"))
                .when(expertProfilePolicy).checkCanEditProfile(testUserId, testProfileId);

        // When & Then
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> expertService.updateProfile(testUserId, updateRequest)
        );
    }

    @Test
    void getPublicProfile_success() {
        // Given
        ExpertProfile verifiedProfile = testProfile.toBuilder()
                .isVerified(true)
                .build();
        when(expertProfileRepository.findById(testProfileId)).thenReturn(Optional.of(verifiedProfile));
        when(expertProfileMapper.toPublicResponse(verifiedProfile)).thenReturn(
                ExpertProfilePublicResponse.builder()
                        .id(testProfileId)
                        .isVerified(true)
                        .build()
        );

        // When
        ExpertProfilePublicResponse response = expertService.getPublicProfile(testProfileId);

        // Then
        assertNotNull(response);
        assertTrue(response.getIsVerified());
    }

    @Test
    void getPublicProfile_notVerified_throws() {
        // Given
        when(expertProfileRepository.findById(testProfileId)).thenReturn(Optional.of(testProfile));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> expertService.getPublicProfile(testProfileId)
        );
        assertEquals("Expert profile is not verified", exception.getMessage());
    }

    @Test
    void uploadCredential_success() throws Exception {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("certificate.pdf");
        when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test".getBytes()));

        UploadCredentialRequest credentialRequest = UploadCredentialRequest.builder()
                .credentialType("CERTIFICATION")
                .issuingAuthority("Medical Board")
                .build();

        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(expertProfileRepository.save(any(ExpertProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expertCredentialRepository.save(any(ExpertCredential.class))).thenAnswer(invocation -> {
            ExpertCredential c = invocation.getArgument(0);
            return c.toBuilder().id(UUID.randomUUID()).build();
        });
        when(expertCredentialMapper.toResponse(any(ExpertCredential.class))).thenReturn(
                ExpertCredentialResponse.builder()
                        .credentialType("CERTIFICATION")
                        .build()
        );

        // When
        ExpertCredentialResponse response = expertService.uploadCredential(testUserId, mockFile, credentialRequest);

        // Then
        assertNotNull(response);
        verify(auditService).log(any(), eq(testUserId), eq("expert_credential"), any(), isNull());
    }

    @Test
    void getMyCredentials_success() {
        // Given
        ExpertCredential credential1 = ExpertCredential.builder()
                .id(UUID.randomUUID())
                .credentialType("TYPE1")
                .build();
        ExpertCredential credential2 = ExpertCredential.builder()
                .id(UUID.randomUUID())
                .credentialType("TYPE2")
                .build();

        when(expertProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(expertCredentialRepository.findByExpertProfileId(testProfileId)).thenReturn(List.of(credential1, credential2));
        when(expertCredentialMapper.toResponseList(List.of(credential1, credential2))).thenReturn(
                List.of(
                        ExpertCredentialResponse.builder().credentialType("TYPE1").build(),
                        ExpertCredentialResponse.builder().credentialType("TYPE2").build()
                )
        );

        // When
        List<ExpertCredentialResponse> responses = expertService.getMyCredentials(testUserId);

        // Then
        assertEquals(2, responses.size());
    }
}
