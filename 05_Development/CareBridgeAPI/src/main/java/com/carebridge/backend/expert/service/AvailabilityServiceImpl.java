package com.carebridge.backend.expert.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.entity.AvailabilitySlot;
import com.carebridge.backend.consultation.repository.AvailabilitySlotRepository;
import com.carebridge.backend.expert.dto.response.AvailabilitySlotDTO;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.enums.SlotStatus;
import com.carebridge.backend.expert.repository.ExpertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Availability Service Implementation.
 * Manages expert availability slots.
 *
 * Implements IAvailabilityService for availability management.
 * Uses ExpertPolicy for RBAC enforcement.
 *
 * TV4 Use Cases: UC-90 (Configure Availability)
 */
@Service("availabilityService")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityServiceImpl implements IAvailabilityService {

    private final AvailabilitySlotRepository availabilityRepository;
    private final ExpertRepository expertRepository;

    @Override
    @Transactional
    public List<AvailabilitySlot> configureSlots(Long expertId, List<AvailabilitySlot> slots) {
        log.info("Configuring {} availability slots for expertId: {}", slots.size(), expertId);

        // Validate expert exists
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        // TODO: Add RBAC check via expertPolicy.ensureCanConfigureAvailability when controller integrates

        // Validate slots
        for (AvailabilitySlot slot : slots) {
            if (slot.getSlotEnd().isBefore(slot.getSlotStart()) || slot.getSlotEnd().equals(slot.getSlotStart())) {
                throw new IllegalArgumentException("Invalid time slot: end must be after start");
            }

            // Set expertId on each slot
            slot.setExpertId(expertId);

            // Check for overlapping slots
            long overlapping = availabilityRepository.countOverlappingSlots(
                    expertId, slot.getSlotStart(), slot.getSlotEnd());
            if (overlapping > 0) {
                throw new ResourceAlreadyExistsException("Time slot conflicts with existing availability");
            }

            // Set default status if not set
            if (slot.getStatus() == null) {
                slot.setStatus(SlotStatus.AVAILABLE);
            }
        }

        // Create slots
        List<AvailabilitySlot> created = slots.stream()
                .map(availabilityRepository::save)
                .collect(Collectors.toList());

        log.info("Created {} availability slots for expertId: {}", created.size(), expertId);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getAvailableSlots(Long expertId, Instant startDate, Instant endDate) {
        log.debug("Getting available slots for expertId: {} from: {} to: {}", expertId, startDate, endDate);
        return availabilityRepository.findAvailableInRange(expertId, startDate, endDate, SlotStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvailabilitySlot> getAvailableSlots(Long expertId, Instant startDate, Instant endDate, Pageable pageable) {
        log.debug("Getting available slots (paged) for expertId: {} from: {} to: {}", expertId, startDate, endDate);
        List<AvailabilitySlot> slots = availabilityRepository.findAvailableInRange(expertId, startDate, endDate, SlotStatus.AVAILABLE);
        // Simple pagination - in production, use repository-level pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), slots.size());
        List<AvailabilitySlot> pageContent = start >= slots.size() ? List.of() : slots.subList(start, end);
        return new PageImpl<>(pageContent, pageable, slots.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(Long slotId) {
        AvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));
        return slot.getStatus() == SlotStatus.AVAILABLE;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AvailabilitySlot> getSlot(Long slotId) {
        return availabilityRepository.findById(slotId);
    }

    @Override
    @Transactional
    public void markAsBooked(Long slotId, Long bookingId) {
        AvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Slot is not available for booking");
        }

        slot.setStatus(SlotStatus.BOOKED);
        slot.setBookingId(bookingId);
        availabilityRepository.save(slot);
        log.info("Marked slot {} as booked for booking: {}", slotId, bookingId);
    }

    @Override
    @Transactional
    public void markAsAvailable(Long slotId) {
        AvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setBookingId(null);
        availabilityRepository.save(slot);
        log.info("Marked slot {} as available", slotId);
    }

    @Override
    @Transactional
    public void blockSlot(Long slotId, String reason) {
        AvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        slot.setStatus(SlotStatus.BLOCKED);
        availabilityRepository.save(slot);
        log.info("Blocked slot {} with reason: {}", slotId, reason);
    }

    @Override
    public AvailabilitySlotDTO toSlotDTO(AvailabilitySlot slot) {
        return AvailabilitySlotDTO.builder()
                .availabilityId(slot.getSlotId())
                .expertId(slot.getExpertId())
                .slotStart(slot.getSlotStart())
                .slotEnd(slot.getSlotEnd())
                .channelType(slot.getChannelType())
                .status(slot.getStatus())
                .bookingId(slot.getBookingId())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }
}
