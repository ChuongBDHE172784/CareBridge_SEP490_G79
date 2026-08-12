package com.carebridge.backend.expertavailability.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertavailability.dto.request.ShareLocationRequest;
import com.carebridge.backend.expertavailability.dto.request.HourlyAvailabilitySlotRequest;
import com.carebridge.backend.expertavailability.dto.request.ReplaceAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.response.AvailabilityResponse;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import com.carebridge.backend.expertavailability.mapper.ExpertAvailabilityMapper;
import com.carebridge.backend.expertavailability.mapper.ExpertLocationShareMapper;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.expertavailability.repository.ExpertLocationShareRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ExpertAvailabilityServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock private ExpertAvailabilityRepository availabilityRepository;
    @Mock private ExpertLocationShareRepository locationShareRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ExpertAvailabilityMapper availabilityMapper;
    @Mock private ConsentGrantRepository consentGrantRepository;

    private ExpertAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExpertAvailabilityServiceImpl(
                availabilityRepository,
                locationShareRepository,
                expertProfileRepository,
                availabilityMapper,
                new ExpertLocationShareMapper(),
                consentGrantRepository,
                CLOCK);
    }

    @Test
    void replaceAvailabilityMaterializesHourlySlotsAndClearsEveryTargetDate() {
        UUID profileId = UUID.randomUUID();
        when(expertProfileRepository.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(eligibleProfile(profileId, UUID.randomUUID())));
        when(availabilityRepository
                .findByExpertProfileIdAndStartAtGreaterThanEqualAndStartAtLessThan(
                        org.mockito.ArgumentMatchers.eq(profileId), any(), any()))
                .thenReturn(List.of());
        when(availabilityRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilityMapper.toResponse(any(ExpertAvailability.class)))
                .thenAnswer(invocation -> {
                    ExpertAvailability row = invocation.getArgument(0);
                    return AvailabilityResponse.builder()
                            .expertProfileId(profileId)
                            .startAt(row.getStartAt())
                            .endAt(row.getEndAt())
                            .build();
                });
        ReplaceAvailabilityRequest request = ReplaceAvailabilityRequest.builder()
                .targetDates(List.of(LocalDate.of(2026, 7, 26), LocalDate.of(2026, 7, 27)))
                .timeZone("UTC")
                .channelType("ONLINE_CHAT")
                .slots(List.of(
                        new HourlyAvailabilitySlotRequest(LocalTime.of(7, 0)),
                        new HourlyAvailabilitySlotRequest(LocalTime.of(20, 0))))
                .build();

        List<AvailabilityResponse> result = service.replaceAvailability(profileId, request);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(AvailabilityResponse::getStartAt).containsExactly(
                Instant.parse("2026-07-26T07:00:00Z"),
                Instant.parse("2026-07-26T20:00:00Z"),
                Instant.parse("2026-07-27T07:00:00Z"),
                Instant.parse("2026-07-27T20:00:00Z"));
        assertThat(result).allSatisfy(slot ->
                assertThat(slot.getEndAt()).isEqualTo(slot.getStartAt().plusSeconds(3600)));
        verify(availabilityRepository, times(2))
                .findByExpertProfileIdAndStartAtGreaterThanEqualAndStartAtLessThan(
                        org.mockito.ArgumentMatchers.eq(profileId), any(), any());
        verify(availabilityRepository, times(2)).deleteAll(any());
    }

    @Test
    void replaceAvailabilityRejectsNonHourlyOrOutOfRangeSlotsBeforeWriting() {
        UUID profileId = UUID.randomUUID();
        when(expertProfileRepository.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(eligibleProfile(profileId, UUID.randomUUID())));
        ReplaceAvailabilityRequest request = ReplaceAvailabilityRequest.builder()
                .targetDates(List.of(LocalDate.of(2026, 7, 26)))
                .timeZone("UTC")
                .channelType("ONLINE_CHAT")
                .slots(List.of(new HourlyAvailabilitySlotRequest(LocalTime.of(20, 30))))
                .build();

        assertExpertError(
                () -> service.replaceAvailability(profileId, request),
                HttpStatus.BAD_REQUEST,
                "EXPERT-011");

        verify(availabilityRepository, never()).deleteAll(any());
        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void publicAvailabilityOnlyReturnsFutureAvailableSlotsForEligibleExpert() {
        UUID profileId = UUID.randomUUID();
        ExpertAvailability available = ExpertAvailability.builder()
                .expertProfileId(profileId)
                .startAt(NOW.plusSeconds(3600))
                .endAt(NOW.plusSeconds(7200))
                .status(com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus.AVAILABLE)
                .build();
        ExpertAvailability busy = ExpertAvailability.builder()
                .expertProfileId(profileId)
                .startAt(NOW.plusSeconds(7200))
                .endAt(NOW.plusSeconds(10800))
                .status(com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus.BUSY)
                .build();
        ExpertAvailability alreadyStarted = ExpertAvailability.builder()
                .expertProfileId(profileId)
                .startAt(NOW.minusSeconds(1800))
                .endAt(NOW.plusSeconds(1800))
                .status(com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus.AVAILABLE)
                .build();
        when(expertProfileRepository.findById(profileId))
                .thenReturn(Optional.of(eligibleProfile(profileId, UUID.randomUUID())));
        when(availabilityRepository.findByExpertProfileIdAndEndAtAfterOrderByStartAtAsc(profileId, NOW))
                .thenReturn(List.of(alreadyStarted, available, busy));
        when(availabilityMapper.toResponse(available))
                .thenReturn(AvailabilityResponse.builder().expertProfileId(profileId).startAt(available.getStartAt()).build());

        assertThat(service.getPublicAvailability(profileId))
                .singleElement()
                .extracting(AvailabilityResponse::getStartAt)
                .isEqualTo(available.getStartAt());
    }

    @Test
    void onlineStatusUpdatesNewestUnexpiredShareWithOwnerScopedConsent() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ExpertLocationShare share = activeShare(profileId, consentId);
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(share));
        when(consentGrantRepository.existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300)))
                .thenReturn(true);
        when(locationShareRepository.save(share)).thenReturn(share);

        LocationShareResponse response = service.setOnlineStatus(profileId, true);

        assertThat(response.getAvailabilityStatus()).isEqualTo("ONLINE");
        assertThat(share.getAvailabilityStatus()).isEqualTo("ONLINE");
        verify(consentGrantRepository)
                .existsValidConsentByPermissionIdCoveringInterval(
                        consentId,
                        userId,
                        ConsentDataType.LOCATION.name(),
                        ConsentPurpose.SHARE.name(),
                        NOW,
                        NOW.plusSeconds(300));
        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository).save(share);
        InOrder onlineOrder = inOrder(consentGrantRepository, locationShareRepository);
        onlineOrder.verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        onlineOrder.verify(locationShareRepository)
                .findTopByExpertProfileIdOrderByCreatedAtDesc(profileId);
        onlineOrder.verify(consentGrantRepository).existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300));
        onlineOrder.verify(locationShareRepository).save(share);
    }

    @Test
    void onlineStatusRejectsBothUnapprovedAndUntrustedProfilesBeforeReadingLocation() {
        UUID profileId = UUID.randomUUID();
        ExpertProfile pending = eligibleProfile(profileId, UUID.randomUUID());
        pending.setVerificationStatus(VerificationStatus.PENDING);
        ExpertProfile suspended = eligibleProfile(profileId, UUID.randomUUID());
        suspended.setTrustStatus(TrustStatus.SUSPENDED);
        when(expertProfileRepository.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(pending), Optional.of(suspended));

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.FORBIDDEN,
                "EXPERT-010");
        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.FORBIDDEN,
                "EXPERT-010");

        verify(expertProfileRepository, times(2)).findByIdForUpdate(profileId);
        verifyNoInteractions(locationShareRepository, consentGrantRepository);
    }

    @Test
    void onlineStatusRejectsExpiredNewestShareWithoutFallingBackOrSaving() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ExpertLocationShare expired = activeShare(profileId, UUID.randomUUID());
        expired.setExpiresAt(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(expired));

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.BAD_REQUEST,
                "EXPERT-013");

        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository, never()).save(expired);
    }

    @Test
    void onlineStatusRejectsShareWithoutExpiry() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ExpertLocationShare share = activeShare(profileId, UUID.randomUUID());
        share.setExpiresAt(null);
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(share));

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.BAD_REQUEST,
                "EXPERT-013");

        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository, never()).save(share);
    }

    @Test
    void onlineStatusRejectsRevokedOrExpiredOwnerConsentWithoutSaving() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ExpertLocationShare share = activeShare(profileId, consentId);
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(share));
        when(consentGrantRepository.existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300)))
                .thenReturn(false);

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.FORBIDDEN,
                "EXPERT-013");

        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository, never()).save(share);
    }

    @Test
    void offlineStatusRemainsAvailableAfterSuspensionExpiryAndConsentRevocation() {
        UUID profileId = UUID.randomUUID();
        ExpertProfile suspended = eligibleProfile(profileId, UUID.randomUUID());
        suspended.setTrustStatus(TrustStatus.SUSPENDED);
        ExpertLocationShare expired = activeShare(profileId, null);
        expired.setExpiresAt(LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC));
        expired.setAvailabilityStatus("ONLINE");
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(suspended));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(expired));
        when(locationShareRepository.save(expired)).thenReturn(expired);

        LocationShareResponse response = service.setOnlineStatus(profileId, false);

        assertThat(response.getAvailabilityStatus()).isEqualTo("OFFLINE");
        assertThat(expired.getAvailabilityStatus()).isEqualTo("OFFLINE");
        verifyNoInteractions(consentGrantRepository);
    }

    @Test
    void onlineStatusRejectsMissingConsentReferenceWithoutQueryingConsent() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ExpertLocationShare share = activeShare(profileId, null);
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(share));

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.FORBIDDEN,
                "EXPERT-013");

        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository, never()).save(share);
    }

    @Test
    void onlineStatusDoesNotReactivateADeletedShare() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.empty());

        assertExpertError(
                () -> service.setOnlineStatus(profileId, true),
                HttpStatus.BAD_REQUEST,
                "EXPERT-013");

        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void stopLocationShareDeletesEveryShareSoAnOlderRowCannotBeReactivated() {
        UUID profileId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, UUID.randomUUID());
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));

        service.stopLocationShare(profileId);

        verify(expertProfileRepository).findByIdForUpdate(profileId);
        verify(locationShareRepository).deleteAllByExpertProfileId(profileId);
        verify(locationShareRepository, never())
                .findTopByExpertProfileIdOrderByCreatedAtDesc(profileId);
        InOrder stopOrder = inOrder(expertProfileRepository, locationShareRepository);
        stopOrder.verify(expertProfileRepository).findByIdForUpdate(profileId);
        stopOrder.verify(locationShareRepository).deleteAllByExpertProfileId(profileId);
    }

    @Test
    void stopLocationShareRejectsUnknownProfileWithoutDeleting() {
        UUID profileId = UUID.randomUUID();
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.empty());

        assertExpertError(
                () -> service.stopLocationShare(profileId),
                HttpStatus.NOT_FOUND,
                "EXPERT-004");

        verify(locationShareRepository, never()).deleteAllByExpertProfileId(profileId);
    }

    @Test
    void shareLocationRequiresExactConsentAndAlwaysStartsOffline() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, userId);
        ShareLocationRequest request = ShareLocationRequest.builder()
                .latitude(new BigDecimal("10.762622"))
                .longitude(new BigDecimal("106.660172"))
                .availabilityStatus("ONLINE")
                .expiresAt(LocalDateTime.ofInstant(NOW.plusSeconds(300), ZoneOffset.UTC))
                .consentReference(consentId)
                .build();
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));
        when(consentGrantRepository.existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300)))
                .thenReturn(true);
        when(locationShareRepository.save(any(ExpertLocationShare.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocationShareResponse response = service.shareLocation(profileId, request);

        assertThat(response.getAvailabilityStatus()).isEqualTo("OFFLINE");
        verify(consentGrantRepository).existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300));
        verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        verify(locationShareRepository).deleteAllByExpertProfileId(profileId);
        InOrder shareOrder = inOrder(consentGrantRepository, locationShareRepository);
        shareOrder.verify(consentGrantRepository).acquireLifecycleOwnerLock(userId);
        shareOrder.verify(consentGrantRepository).existsValidConsentByPermissionIdCoveringInterval(
                consentId,
                userId,
                ConsentDataType.LOCATION.name(),
                ConsentPurpose.SHARE.name(),
                NOW,
                NOW.plusSeconds(300));
        shareOrder.verify(locationShareRepository).deleteAllByExpertProfileId(profileId);
        shareOrder.verify(locationShareRepository).save(any(ExpertLocationShare.class));
    }

    @Test
    void shareLocationRejectsMissingOrPastExpiryBeforeSaving() {
        UUID profileId = UUID.randomUUID();
        ExpertProfile profile = eligibleProfile(profileId, UUID.randomUUID());
        ShareLocationRequest request = ShareLocationRequest.builder()
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .expiresAt(LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC))
                .consentReference(UUID.randomUUID())
                .build();
        when(expertProfileRepository.findByIdForUpdate(profileId)).thenReturn(Optional.of(profile));

        assertExpertError(
                () -> service.shareLocation(profileId, request),
                HttpStatus.BAD_REQUEST,
                "EXPERT-013");

        verifyNoInteractions(consentGrantRepository);
        verify(locationShareRepository, never()).save(any());
    }

    private static ExpertProfile eligibleProfile(UUID profileId, UUID userId) {
        return ExpertProfile.builder()
                .expertProfileId(profileId)
                .userId(userId)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    private static ExpertLocationShare activeShare(UUID profileId, UUID consentId) {
        return ExpertLocationShare.builder()
                .locationShareId(UUID.randomUUID())
                .expertProfileId(profileId)
                .consentReference(consentId)
                .createdAt(LocalDateTime.ofInstant(NOW.minusSeconds(60), ZoneOffset.UTC))
                .expiresAt(LocalDateTime.ofInstant(NOW.plusSeconds(300), ZoneOffset.UTC))
                .build();
    }

    private static void assertExpertError(
            Runnable action, HttpStatus status, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ExpertException.class, error -> {
                    assertThat(error.getHttpStatus()).isEqualTo(status);
                    assertThat(error.getCode()).isEqualTo(code);
                });
    }
}
