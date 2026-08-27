package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GrowthChartResponse {

    private UUID babyId;
    private String nickname;
    private LocalDate birthDate;
    private List<GrowthDataPoint> measurements;
}
