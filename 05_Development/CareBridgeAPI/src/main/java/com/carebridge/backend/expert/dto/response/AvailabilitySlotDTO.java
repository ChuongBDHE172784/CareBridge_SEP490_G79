package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Availability slot DTO for responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotDTO {

    private Long availabilityId;
    private Long expertId;
    private Instant slotStart;
    private Instant slotEnd;
    private String channelType;
    private SlotStatus status;
    private Long bookingId;
    private Instant createdAt;
    private Instant updatedAt;
}
