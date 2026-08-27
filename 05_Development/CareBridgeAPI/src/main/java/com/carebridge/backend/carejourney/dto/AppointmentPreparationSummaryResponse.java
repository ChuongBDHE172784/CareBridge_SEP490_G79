package com.carebridge.backend.carejourney.dto;

import java.util.List;
import java.util.UUID;

public record AppointmentPreparationSummaryResponse(
        UUID babyId,
        List<String> facts,
        List<String> dueItems,
        String notice) {
}
