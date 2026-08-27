package com.carebridge.backend.safety.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyConfigRequest {

    @NotNull
    private Boolean fallDetectionEnabled;

    @NotNull
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "sensitivityLevel must be LOW, MEDIUM, or HIGH")
    private String sensitivityLevel;

    @NotNull
    private Boolean emergencyAutoAlert;

    /** Explicit opt-in for attaching location to fall emergency alerts. */
    private Boolean locationSharingEnabled;

    /** Additive for old clients; omitted values retain the stored/default countdown. */
    private Integer countdownSeconds;

    /** OS permission attestation. Consent is checked independently server-side. */
    private Boolean sensorPermissionGranted;
}
