package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.AppointmentPreparationSummaryResponse;
import java.util.UUID;

public interface IAppointmentPreparationService {
    AppointmentPreparationSummaryResponse getSummary(UUID babyId, UUID callerId);
}
