package com.carebridge.backend.community.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityProfileRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityProfileRequest;
import com.carebridge.backend.community.dto.response.CommunityProfileResponse;
import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.exception.CommunityProfileAlreadyExistsException;
import com.carebridge.backend.community.exception.CommunityProfileNotFoundException;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.community.service.CommunityProfileService;
import com.carebridge.backend.community.service.CommunityProfileServiceImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * COMM-TC-020-001..006 and COMM-TC-021-001..007 (UC20/UC21 Test-Specs §4). Both UCs share
 * {@code CommunityProfileServiceImpl}, so both are covered in this one test class.
 */
@ExtendWith(MockitoExtension.class)
class CommunityProfileServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock private CommunityProfileRepository profileRepository;
    @Mock private AuditService auditService;

    private CommunityProfileService newService() {
        return new CommunityProfileServiceImpl(profileRepository, auditService);
    }

    private CreateCommunityProfileRequest makeCreateRequest() {
        CreateCommunityProfileRequest req = new CreateCommunityProfileRequest();
        req.setDisplayName("TestMother20");
        req.setBio("This is a synthetic test bio");
        req.setInterestStage(PregnancyStage.PREGNANCY);
        req.setVisible(true);
        req.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/test-20.jpg");
        req.setRegion("Hà Nội, Việt Nam");
        return req;
    }

    /** Canonical model: the community profile IS the users row — createProfile loads the
     * existing account row via findAccountByUserId and fills in the community fields. */
    private CommunityProfile makeAccountRow(UUID userId) {
        return CommunityProfile.builder()
                .communityProfileId(userId)
                .userId(userId)
                .createdAt(Instant.now())
                .build();
    }

    private CommunityProfile makeSavedProfile(UUID userId) {
        return CommunityProfile.builder()
                .communityProfileId(UUID.randomUUID())
                .userId(userId)
                .displayName("TestMother20")
                .bio("This is a synthetic test bio")
                .interestStage("PREGNANCY")
                .visible(true)
                .publicAvatarUrl("https://storage.carebridge.vn/avatars/test-20.jpg")
                .region("Hà Nội, Việt Nam")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── UC-20 Create ──────────────────────────────────────────────────────

    // COMM-TC-020-001: happy path
    @Test
    void createProfile_validRequest_returnsProfileAndEmitsAudit() {
        when(profileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(profileRepository.findAccountByUserId(USER_ID)).thenReturn(Optional.of(makeAccountRow(USER_ID)));
        when(profileRepository.save(any(CommunityProfile.class))).thenReturn(makeSavedProfile(USER_ID));

        CommunityProfileResponse result = newService().createProfile(USER_ID, makeCreateRequest());

        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("TestMother20");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.isVisible()).isTrue();
        assertThat(result.getCommunityProfileId()).isNotNull();

        verify(auditService).log(eq(AuditAction.COMMUNITY_PROFILE_CREATED), eq(USER_ID), any(), any(), any());
        verify(profileRepository).save(any(CommunityProfile.class));
    }

    @Test
    void createProfile_legacyBabyCareStage_isPersistedAndReturnedAsPostpartum() {
        CreateCommunityProfileRequest request = makeCreateRequest();
        request.setInterestStage(PregnancyStage.fromApiValue("BABY_CARE"));
        CommunityProfile account = makeAccountRow(USER_ID);
        when(profileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(profileRepository.findAccountByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

        CommunityProfileResponse result = newService().createProfile(USER_ID, request);

        assertThat(account.getInterestStage()).isEqualTo("POSTPARTUM");
        assertThat(result.getInterestStage()).isEqualTo(PregnancyStage.POSTPARTUM);
    }

    // COMM-TC-020-002: duplicate → COMM-001
    @Test
    void createProfile_duplicateProfile_throwsConflictException() {
        when(profileRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> newService().createProfile(USER_ID, makeCreateRequest()))
                .isInstanceOf(CommunityProfileAlreadyExistsException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(profileRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // COMM-TC-020-005: isVisible defaults to true (DTO default; entity captured must match)
    @Test
    void createProfile_isVisibleDefaultsToTrue_whenNotProvided() {
        CreateCommunityProfileRequest request = new CreateCommunityProfileRequest();
        request.setDisplayName("DefaultVisibilityUser");

        assertThat(request.isVisible()).isTrue();

        when(profileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(profileRepository.findAccountByUserId(USER_ID)).thenReturn(Optional.of(makeAccountRow(USER_ID)));
        when(profileRepository.save(any(CommunityProfile.class))).thenReturn(makeSavedProfile(USER_ID));

        CommunityProfileResponse result = newService().createProfile(USER_ID, request);

        assertThat(result.isVisible()).isTrue();
        ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().isVisible()).isTrue();
    }

    // COMM-TC-020-006: userId comes from the method parameter (JWT), never the request body
    @Test
    void createProfile_userIdFromParameter_notFromRequestBody() {
        when(profileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(profileRepository.findAccountByUserId(USER_ID)).thenReturn(Optional.of(makeAccountRow(USER_ID)));
        when(profileRepository.save(any(CommunityProfile.class))).thenReturn(makeSavedProfile(USER_ID));

        newService().createProfile(USER_ID, makeCreateRequest());

        ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    // ── UC-21 Update ──────────────────────────────────────────────────────

    private CommunityProfile makeExistingProfile(UUID userId) {
        return CommunityProfile.builder()
                .communityProfileId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567821"))
                .userId(userId)
                .displayName("OriginalDisplayName")
                .bio("Original bio")
                .interestStage("PREGNANCY")
                .visible(true)
                .publicAvatarUrl("https://storage.carebridge.vn/avatars/user-21.jpg")
                .region("Hà Nội, Việt Nam")
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();
    }

    private UpdateCommunityProfileRequest makeUpdateRequest() {
        UpdateCommunityProfileRequest req = new UpdateCommunityProfileRequest();
        req.setDisplayName("UpdatedDisplayName21");
        req.setBio("Updated bio content");
        req.setInterestStage(PregnancyStage.POSTPARTUM);
        req.setVisible(true);
        req.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/user-21-v2.jpg");
        req.setRegion("TP. Hồ Chí Minh");
        return req;
    }

    // COMM-TC-021-001: happy path update
    @Test
    void updateProfile_validRequest_returnsUpdatedProfile() {
        CommunityProfile existing = makeExistingProfile(USER_ID);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

        CommunityProfileResponse result = newService().updateProfile(USER_ID, makeUpdateRequest());

        assertThat(result.getDisplayName()).isEqualTo("UpdatedDisplayName21");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.isVisible()).isTrue();

        verify(profileRepository).findByUserId(USER_ID);
        verify(profileRepository).save(any(CommunityProfile.class));
        verify(auditService).log(eq(AuditAction.COMMUNITY_PROFILE_UPDATED), eq(USER_ID), any(), any(), any());
    }

    // COMM-TC-021-002: hide profile — is_visible=false, never deleted
    @Test
    void updateProfile_hideProfile_setsVisibleFalse_doesNotDelete() {
        CommunityProfile existing = makeExistingProfile(USER_ID);
        UpdateCommunityProfileRequest request = makeUpdateRequest();
        request.setVisible(false);

        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

        CommunityProfileResponse result = newService().updateProfile(USER_ID, request);

        assertThat(result.isVisible()).isFalse();
        assertThat(result.getCommunityProfileId()).isNotNull();
        verify(profileRepository, never()).delete(any());
        verify(profileRepository, never()).deleteById(any());
        verify(profileRepository).save(any(CommunityProfile.class));

        ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().isVisible()).isFalse();
    }

    // COMM-TC-021-003: profile not found → COMM-011
    @Test
    void updateProfile_profileNotFound_throwsNotFoundException() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().updateProfile(USER_ID, makeUpdateRequest()))
                .isInstanceOf(CommunityProfileNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(profileRepository, never()).save(any());
        verify(profileRepository, never()).delete(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // COMM-TC-021-006: PUT semantics — fields not sent are cleared to null
    @Test
    void updateProfile_putSemantics_nullFieldsAreCleared() {
        CommunityProfile existing = makeExistingProfile(USER_ID); // has bio and region set

        UpdateCommunityProfileRequest request = new UpdateCommunityProfileRequest();
        request.setDisplayName("OnlyDisplayName");
        request.setVisible(true);
        // bio = null, region = null intentionally

        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

        newService().updateProfile(USER_ID, request);

        ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
        verify(profileRepository).save(captor.capture());
        CommunityProfile saved = captor.getValue();

        assertThat(saved.getDisplayName()).isEqualTo("OnlyDisplayName");
        assertThat(saved.getBio()).isNull();
        assertThat(saved.getRegion()).isNull();
    }

    // COMM-TC-021-007: updatedAt changes after update
    @Test
    void updateProfile_updatesUpdatedAt() {
        CommunityProfile existing = makeExistingProfile(USER_ID);
        Instant originalUpdatedAt = existing.getUpdatedAt();

        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

        Instant beforeUpdate = Instant.now();
        newService().updateProfile(USER_ID, makeUpdateRequest());

        ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
        verify(profileRepository).save(captor.capture());
        CommunityProfile saved = captor.getValue();

        assertThat(saved.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}
