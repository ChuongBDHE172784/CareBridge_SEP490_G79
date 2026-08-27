package com.carebridge.backend.vaccination.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VaccinationScheduleResponse {

    private UUID babyId;
    private String babyNickname;
    private List<VaccinationDoseDto> doses;
}
