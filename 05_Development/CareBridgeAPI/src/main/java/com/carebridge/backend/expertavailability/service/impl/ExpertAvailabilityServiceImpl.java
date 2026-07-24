package com.carebridge.backend.expertavailability.service.impl;

import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.request.SetOnlineStatusRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertAvailabilityServiceImpl implements IExpertAvailabilityService {

    private final ExpertAvailabilityRepository availabilityRepository;
    private final ExpertLocationShareRepository locationShareRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertAvailabilityMapper availabilityMapper;
    private final ExpertLocationShareMapper locationShareMapper;

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

        if (request.getLatitude().compareTo(new java.math.BigDecimal("90.1")) > 0
                || request.getLatitude().compareTo(new java.math.BigDecimal("-90.1")) < 0) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-014", "Invalid latitude");
        }
        if (request.getLongitude().compareTo(new java.math.BigDecimal("180.1")) > 0
                || request.getLongitude().compareTo(new java.math.BigDecimal("-180.1")) < 0) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-014", "Invalid longitude");
        }

        var profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        var locationShare = locationShareMapper.toEntity(expertProfileId, request);
        var saved = locationShareRepository.save(locationShare);
        return locationShareMapper.toResponse(saved);
    }

    @Override
    public void stopLocationShare(UUID expertProfileId) {
        locationShareRepository.findTopByExpertProfileIdOrderByCreatedAtDesc(expertProfileId)
                .ifPresent(locationShareRepository::delete);
    }

    @Override
    public LocationShareResponse setOnlineStatus(UUID expertProfileId, Boolean online) {
        var profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "EXPERT-010", "Expert profile not verified");
        }
        var latest = locationShareRepository
                .findTopByExpertProfileIdOrderByCreatedAtDesc(expertProfileId)
                .orElse(null);
        if (latest == null) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-013", "Share location first before setting online status");
        }
        latest.setAvailabilityStatus(online ? "ONLINE" : "OFFLINE");
        var saved = locationShareRepository.save(latest);
        return locationShareMapper.toResponse(saved);
    }
}
