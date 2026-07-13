package com.carebridge.backend.emergency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
    private String relationship;
    private boolean primaryContact;
    private Instant updatedAt;
}
