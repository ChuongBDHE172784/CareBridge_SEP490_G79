package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.entity.AvailabilitySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Availability Service Interface.
 * Defines contract for expert availability management.
 *
 * ISP: Interface segregation - only availability-specific methods.
 */
public interface IAvailabilityService {

    /**
     * Configure availability slots for an expert.
     *
     * @param expertId Expert ID
     * @param slots List of availability slots
     * @return List of created slots
     */
    List<AvailabilitySlot> configureSlots(Long expertId, List<AvailabilitySlot> slots);

    /**
     * Get available slots for an expert in a time range.
     *
     * @param expertId Expert ID
     * @param startDate Start date filter
     * @param endDate End date filter
     * @return List of available slots
     */
    List<AvailabilitySlot> getAvailableSlots(Long expertId, Instant startDate, Instant endDate);

    /**
     * Get available slots with pagination.
     *
     * @param expertId Expert ID
     * @param startDate Start date filter
     * @param endDate End date filter
     * @param pageable Pagination parameters
     * @return Page of available slots
     */
    Page<AvailabilitySlot> getAvailableSlots(Long expertId, Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Check if a slot is available for booking.
     *
     * @param slotId Slot ID
     * @return true if available, false otherwise
     */
    boolean isSlotAvailable(Long slotId);

    /**
     * Get a slot by ID.
     *
     * @param slotId Slot ID
     * @return Optional of slot
     */
    Optional<AvailabilitySlot> getSlot(Long slotId);

    /**
     * Mark a slot as booked.
     *
     * @param slotId Slot ID
     * @param bookingId Associated booking ID
     */
    void markAsBooked(Long slotId, Long bookingId);

    /**
     * Mark a slot as available (unbook).
     *
     * @param slotId Slot ID
     */
    void markAsAvailable(Long slotId);

    /**
     * Block a slot (make unavailable).
     *
     * @param slotId Slot ID
     * @param reason Reason for blocking
     */
    void blockSlot(Long slotId, String reason);

    /**
     * Convert AvailabilitySlot entity to DTO.
     *
     * @param slot The slot entity
     * @return AvailabilitySlotDTO
     */
    AvailabilitySlotDTO toSlotDTO(AvailabilitySlot slot);
}
