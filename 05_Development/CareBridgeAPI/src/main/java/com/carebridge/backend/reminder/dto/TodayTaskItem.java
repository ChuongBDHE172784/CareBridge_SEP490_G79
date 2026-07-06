package com.carebridge.backend.reminder.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TodayTaskItem {

    private UUID id;
    private String type;        // VACCINATION, MEDICATION, APPOINTMENT, CARE_TASK
    private String title;
    private Instant scheduledAt;
    private String status;
    private int priority;       // 1=VACCINATION, 2=MEDICATION, 3=APPOINTMENT, 4=CARE_TASK
}
