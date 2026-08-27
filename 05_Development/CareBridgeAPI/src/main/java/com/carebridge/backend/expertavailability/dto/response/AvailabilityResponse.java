package com.carebridge.backend.expertavailability.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private UUID availabilityId;
    private UUID expertProfileId;
    private Instant startAt;
    private Instant endAt;
    private String channelType;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
