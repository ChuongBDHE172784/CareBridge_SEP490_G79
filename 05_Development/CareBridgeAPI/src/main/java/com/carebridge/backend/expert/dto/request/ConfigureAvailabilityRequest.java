package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.enums.SlotStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for configuring availability slots.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureAvailabilityRequest {

    /**
     * List of availability slots to configure.
     */
    @NotEmpty(message = "At least one slot is required")
    private List<SlotConfigDTO> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotConfigDTO {
        /**
         * Start time of the slot.
         */
        @NotNull(message = "Slot start time is required")
        private Instant slotStart;

        /**
         * End time of the slot.
         */
        @NotNull(message = "Slot end time is required")
        private Instant slotEnd;

        /**
         * Consultation modality.
         */
        private String channelType;

        /**
         * Optional slot status (default: AVAILABLE).
         */
        @Builder.Default
        private SlotStatus status = SlotStatus.AVAILABLE;
    }
}
