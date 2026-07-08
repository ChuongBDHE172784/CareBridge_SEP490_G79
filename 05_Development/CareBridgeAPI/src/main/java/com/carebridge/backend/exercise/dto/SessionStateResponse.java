package com.carebridge.backend.exercise.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStateResponse {

    private UUID exerciseSessionId;
    private String sessionStatus;
    private Integer pausedSeconds;
    private Integer warningCount;
    private OffsetDateTime updatedAt;
}
