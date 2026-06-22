package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expert.dto.request.AvailabilitySlotRequest;
import com.carebridge.backend.expert.dto.response.AvailabilitySlotResponse;
import com.carebridge.backend.expert.entity.ExpertAvailability;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.AvailabilityMapper;
import com.carebridge.backend.expert.policy.AvailabilityPolicy;
import com.carebridge.backend.expert.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final ExpertAvailabilityRepository availabilityRepository;
    private final ExpertProfileRepository profileRepository;
    private final AvailabilityMapper availabilityMapper;
    private final AvailabilityPolicy availabilityPolicy;
    private final AuditService auditService;

    @Override
    public List<AvailabilitySlotResponse> getMyAvailability(UUID userId) {
        ExpertProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));
        List<ExpertAvailability> slots = availabilityRepository.findByExpertProfileIdAndIsActiveTrue(profile.getId());
        return availabilityMapper.toResponseList(slots);
    }

    @Override
    public AvailabilitySlotResponse createSlot(UUID userId, AvailabilitySlotRequest request) {
        ExpertProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        availabilityPolicy.checkCanEditAvailability(userId, profile.getId());

        ExpertAvailability slot = ExpertAvailability.builder()
                .expertProfileId(profile.getId())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Hanoi")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ExpertAvailability saved = availabilityRepository.save(slot);

        auditService.log(AuditAction.AVAILABILITY_SLOT_CREATED, userId, "availability_slot", saved.getId().toString(), null);

        return availabilityMapper.toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse updateSlot(UUID userId, UUID slotId, AvailabilitySlotRequest request) {
        ExpertAvailability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));

        ExpertProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        availabilityPolicy.checkCanEditAvailability(userId, profile.getId());

        if (!slot.getExpertProfileId().equals(profile.getId())) {
            throw new IllegalArgumentException("Cannot update slot of another expert");
        }

        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        if (request.getTimezone() != null) slot.setTimezone(request.getTimezone());
        slot.setUpdatedAt(Instant.now());

        ExpertAvailability saved = availabilityRepository.save(slot);

        auditService.log(AuditAction.AVAILABILITY_SLOT_UPDATED, userId, "availability_slot", slotId.toString(), null);

        return availabilityMapper.toResponse(saved);
    }

    @Override
    public void deleteSlot(UUID userId, UUID slotId) {
        ExpertAvailability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));

        ExpertProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        availabilityPolicy.checkCanEditAvailability(userId, profile.getId());

        if (!slot.getExpertProfileId().equals(profile.getId())) {
            throw new IllegalArgumentException("Cannot delete slot of another expert");
        }

        availabilityRepository.delete(slot);

        auditService.log(AuditAction.AVAILABILITY_SLOT_DELETED, userId, "availability_slot", slotId.toString(), null);
    }
}
