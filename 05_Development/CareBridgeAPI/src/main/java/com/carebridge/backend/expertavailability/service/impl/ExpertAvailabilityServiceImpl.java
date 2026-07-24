package com.carebridge.backend.expertavailability.service.impl;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.request.ShareLocationRequest;
import com.carebridge.backend.expertavailability.dto.response.AvailabilityResponse;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import com.carebridge.backend.expertavailability.mapper.ExpertAvailabilityMapper;
import com.carebridge.backend.expertavailability.mapper.ExpertLocationShareMapper;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.expertavailability.repository.ExpertLocationShareRepository;
import com.carebridge.backend.expertavailability.service.IExpertAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpertAvailabilityServiceImpl implements IExpertAvailabilityService {

    private final ExpertAvailabilityRepository availabilityRepository;
    private final ExpertLocationShareRepository locationShareRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertAvailabilityMapper availabilityMapper;
    private final ExpertLocationShareMapper locationShareMapper;
    private final ConsentGrantRepository consentGrantRepository;
    private final Clock clock;

    @Autowired
    public ExpertAvailabilityServiceImpl(
            ExpertAvailabilityRepository availabilityRepository,
            ExpertLocationShareRepository locationShareRepository,
            ExpertProfileRepository expertProfileRepository,
            ExpertAvailabilityMapper availabilityMapper,
            ExpertLocationShareMapper locationShareMapper,
            ConsentGrantRepository consentGrantRepository) {
        this(
                availabilityRepository,
                locationShareRepository,
                expertProfileRepository,
                availabilityMapper,
                locationShareMapper,
                consentGrantRepository,
                Clock.systemDefaultZone());
    }

    public ExpertAvailabilityServiceImpl(
            ExpertAvailabilityRepository availabilityRepository,
            ExpertLocationShareRepository locationShareRepository,
            ExpertProfileRepository expertProfileRepository,
            ExpertAvailabilityMapper availabilityMapper,
            ExpertLocationShareMapper locationShareMapper,
            ConsentGrantRepository consentGrantRepository,
            Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.locationShareRepository = locationShareRepository;
        this.expertProfileRepository = expertProfileRepository;
        this.availabilityMapper = availabilityMapper;
        this.locationShareMapper = locationShareMapper;
        this.consentGrantRepository = consentGrantRepository;
        this.clock = clock;
    }

    @Override
    public AvailabilityResponse createAvailability(UUID expertProfileId, CreateAvailabilityRequest request) {
        if (request.getEndAt().isBefore(request.getStartAt()) || request.getEndAt().equals(request.getStartAt())) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-011", "endAt must be after startAt");
        }

        var profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-010", "Expert profile not verified");
        }

        var availability = availabilityMapper.toEntity(expertProfileId, request);
        var saved = availabilityRepository.save(availability);
        return availabilityMapper.toResponse(saved);
    }

    @Override
    public List<AvailabilityResponse> getMyAvailability(UUID expertProfileId) {
        return availabilityRepository.findByExpertProfileId(expertProfileId).stream()
                .map(availabilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAvailability(UUID availabilityId, UUID expertProfileId) {
        var availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-010", "Availability not found"));

        if (!availability.getExpertProfileId().equals(expertProfileId)) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-005", "Insufficient permissions");
        }

        availabilityRepository.delete(availability);
    }

    @Override
    public LocationShareResponse shareLocation(UUID expertProfileId, ShareLocationRequest request) {
        if (request.getConsentReference() == null) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-013", "Valid location consent required");
        }

        if (request.getLatitude().compareTo(new java.math.BigDecimal("90")) > 0
                || request.getLatitude().compareTo(new java.math.BigDecimal("-90")) < 0) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-014", "Invalid latitude");
        }
        if (request.getLongitude().compareTo(new java.math.BigDecimal("180")) > 0
                || request.getLongitude().compareTo(new java.math.BigDecimal("-180")) < 0) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-014", "Invalid longitude");
        }

        var profile = expertProfileRepository.findByIdForUpdate(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));
        if (!profile.isEligibleForConsultation()) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-010", "Expert profile is not eligible");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.getExpiresAt() == null || !request.getExpiresAt().isAfter(now)) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-013", "Location share expiry must be in the future");
        }
        consentGrantRepository.acquireLifecycleOwnerLock(profile.getUserId());
        if (!hasValidLocationConsent(
                profile.getUserId(),
                request.getConsentReference(),
                clock.instant(),
                toInstant(request.getExpiresAt()))) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-013", "Valid location consent required");
        }

        locationShareRepository.deleteAllByExpertProfileId(expertProfileId);
        var locationShare = locationShareMapper.toEntity(expertProfileId, request);
        locationShare.setAvailabilityStatus("OFFLINE");
        var saved = locationShareRepository.save(locationShare);
        return locationShareMapper.toResponse(saved);
    }

    @Override
    public void stopLocationShare(UUID expertProfileId) {
        expertProfileRepository.findByIdForUpdate(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));
        locationShareRepository.deleteAllByExpertProfileId(expertProfileId);
    }

    @Override
    public LocationShareResponse setOnlineStatus(UUID expertProfileId, Boolean online) {
        if (online == null) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-014", "Online status is required");
        }
        var profile = expertProfileRepository.findByIdForUpdate(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));
        if (online && !profile.isEligibleForConsultation()) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-010", "Expert profile is not eligible");
        }
        if (online) {
            consentGrantRepository.acquireLifecycleOwnerLock(profile.getUserId());
        }
        var latest = locationShareRepository
                .findTopByExpertProfileIdOrderByCreatedAtDesc(expertProfileId)
                .orElse(null);
        if (latest == null) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-013", "Share location first before setting online status");
        }
        if (!online) {
            latest.setAvailabilityStatus("OFFLINE");
            return locationShareMapper.toResponse(locationShareRepository.save(latest));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (latest.getExpiresAt() == null || !latest.getExpiresAt().isAfter(now)) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-013", "Location share has expired");
        }
        if (latest.getConsentReference() == null) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-013", "Valid location consent required");
        }
        if (!hasValidLocationConsent(
                profile.getUserId(),
                latest.getConsentReference(),
                clock.instant(),
                toInstant(latest.getExpiresAt()))) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-013", "Valid location consent required");
        }
        latest.setAvailabilityStatus("ONLINE");
        var saved = locationShareRepository.save(latest);
        return locationShareMapper.toResponse(saved);
    }

    private boolean hasValidLocationConsent(
            UUID userId,
            UUID permissionId,
            java.time.Instant now,
            java.time.Instant requiredUntil) {
        return permissionId != null
                && consentGrantRepository.existsValidConsentByPermissionIdCoveringInterval(
                        permissionId,
                        userId,
                        ConsentDataType.LOCATION.name(),
                        ConsentPurpose.SHARE.name(),
                        now,
                        requiredUntil);
    }

    private java.time.Instant toInstant(LocalDateTime value) {
        return value.atZone(clock.getZone()).toInstant();
    }
}
