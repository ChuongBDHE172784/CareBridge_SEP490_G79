package com.carebridge.backend.carejourney.dto;

import java.util.UUID;

public record BabyCareOverviewResponse(
        UUID babyId,
        String nickname,
        long journalCount,
        long growthMeasurementCount,
        long milestoneCount,
        long vaccinationRecordCount,
        String notice) {
}
