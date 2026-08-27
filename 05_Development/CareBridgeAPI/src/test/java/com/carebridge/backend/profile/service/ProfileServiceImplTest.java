package com.carebridge.backend.profile.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.profile.dto.ProfileResponse;
import com.carebridge.backend.profile.dto.UpdateProfileRequest;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.profile.service.impl.ProfileServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * UC-09 Update Account Profile — service-level unit tests for the {@code profile} package.
 * Covers TDD cases that exercise business logic in {@link ProfileServiceImpl}
 * (bean-validation-only cases live in {@link com.carebridge.backend.profile.dto.UpdateProfileRequestValidationTest}).
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // CASE 2.0 — Props Isolation: fresh valid request per test, never mutate a shared instance.
    private UpdateProfileRequest buildValidRequest() {
        return new UpdateProfileRequest(
                "Nguyen Test",
                "https://cdn.carebridge.vn/avatars/test.jpg",
                "0912345678",
                LocalDate.of(1995, 6, 15),
                "Ha Noi");
    }

    private void mockAccountUser() {
        User user = User.builder().id(USER_ID).name("Original Name").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // PRF-TC-004 — dateOfBirth in the future is rejected with PRF-002 (Oracle: BR-PRF-DOB).
    @Test
    @DisplayName("PRF-TC-004: Future dateOfBirth is rejected with PRF-002 and nothing is persisted")
    void updateProfile_futureDateOfBirth_throwsPRF002() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null, LocalDate.now().plusDays(1), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(USER_ID, request));

        assertEquals("PRF-002", ex.getCode());
        verify(profileRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // PRF-TC-004 (lower bound) — dateOfBirth before 1900-01-01 is rejected with PRF-002.
    @Test
    @DisplayName("PRF-TC-004b: dateOfBirth before 1900-01-01 is rejected with PRF-002")
    void updateProfile_dateOfBirthBefore1900_throwsPRF002() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null, LocalDate.of(1899, 12, 31), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(USER_ID, request));

        assertEquals("PRF-002", ex.getCode());
        verify(profileRepository, never()).save(any());
    }

    // PRF-TC-009 — a successful update writes exactly one PROFILE_UPDATED audit record (Oracle: ADR-002).
    @Test
    @DisplayName("PRF-TC-009: Successful update writes a PROFILE_UPDATED audit record")
    void updateProfile_success_writesAuditLog() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        mockAccountUser();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            p.setProfileId(profileId);
            return p;
        });

        ProfileResponse response = profileService.updateProfile(USER_ID, buildValidRequest());

        assertNotNull(response);
        assertEquals(USER_ID, response.userId());
        assertEquals("Nguyen Test", response.displayName());
        verify(userRepository).save(argThat(user -> "Nguyen Test".equals(user.getName())));
        verify(profileRepository, times(1)).save(any(UserProfile.class));
        verify(auditService, times(1)).log(
                eq(AuditAction.PROFILE_UPDATED),
                eq(USER_ID),
                eq("UserProfile"),
                eq(profileId.toString()),
                any());
    }

    // PRF-TC-009b — audit is NOT written when persistence fails (transactional integrity, ADR-002).
    @Test
    @DisplayName("PRF-TC-009b: Failed persistence writes no audit record")
    void updateProfile_saveFails_noAuditLog() {
        mockAccountUser();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> profileService.updateProfile(USER_ID, buildValidRequest()));

        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // Supporting invariant — own-resource: the persisted profile always carries the JWT userId (BR-PRF-OWN, C1).
    @Test
    @DisplayName("PRF-TC-006: Persisted profile always uses the authenticated userId")
    void updateProfile_persistsAuthenticatedUserId() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        mockAccountUser();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            p.setProfileId(profileId);
            return p;
        });

        profileService.updateProfile(USER_ID, buildValidRequest());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
    }

    // Supporting invariant — displayName HTML tags are stripped before persistence (C2, OWASP A03).
    @Test
    @DisplayName("PRF-TC-007: HTML tags in displayName are stripped before persistence")
    void updateProfile_htmlInDisplayName_isStripped() {
        UUID profileId = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
        mockAccountUser();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            p.setProfileId(profileId);
            return p;
        });

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Hello <b>World</b>", null, null, null, null);

        ProfileResponse response = profileService.updateProfile(USER_ID, request);

        assertFalse(response.displayName().contains("<"));
        assertFalse(response.displayName().contains(">"));
        verify(userRepository).save(argThat(user -> "Hello World".equals(user.getName())));
    }

    @Test
    @DisplayName("PRF-TC-010: Profile displayName is read from users.full_name")
    void getProfile_returnsCanonicalUserFullName() {
        UserProfile profile = UserProfile.builder()
                .profileId(UUID.fromString("00000000-0000-0000-0000-0000000000dd"))
                .userId(USER_ID)
                .phoneNumber("0912345678")
                .build();
        User user = User.builder().id(USER_ID).name("Canonical Name").build();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ProfileResponse response = profileService.getProfile(USER_ID);

        assertEquals("Canonical Name", response.displayName());
        assertEquals("0912345678", response.phoneNumber());
    }
}
