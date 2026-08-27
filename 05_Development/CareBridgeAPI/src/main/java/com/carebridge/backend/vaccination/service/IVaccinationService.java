package com.carebridge.backend.vaccination.service;

import com.carebridge.backend.vaccination.dto.*;

import java.util.UUID;
import java.util.List;

public interface IVaccinationService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (VAC-001/404) if baby not found */
    VaccinationScheduleResponse getVaccinationSchedule(UUID babyProfileId, UUID callerId);

    List<VaccinationRecordResponse> listVaccinationRecords(UUID babyId, UUID callerId);

    AddVaccinationRecordResponse addVaccinationRecord(UUID babyId, UUID callerId, AddVaccinationRecordRequest request);

    VaccinationRecordResponse updateVaccinationRecord(UUID babyId, UUID recordId, UUID callerId,
                                                      UpdateVaccinationRecordRequest request);

    void deleteVaccinationRecord(UUID babyId, UUID recordId, UUID callerId);

    VaccinationCompletionResponse markVaccinationCompleted(UUID babyId, UUID callerId,
                                                           MarkVaccinationCompletedRequest request);

    PostponeVaccinationResponse postponeVaccination(UUID babyId, UUID callerId, PostponeVaccinationRequest request);
}
