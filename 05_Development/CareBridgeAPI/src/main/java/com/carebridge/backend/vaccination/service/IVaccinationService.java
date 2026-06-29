package com.carebridge.backend.vaccination.service;

import com.carebridge.backend.vaccination.dto.VaccinationScheduleResponse;

import java.util.UUID;

public interface IVaccinationService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (VAC-001/404) if baby not found */
    VaccinationScheduleResponse getVaccinationSchedule(UUID babyProfileId, UUID callerId);
}
