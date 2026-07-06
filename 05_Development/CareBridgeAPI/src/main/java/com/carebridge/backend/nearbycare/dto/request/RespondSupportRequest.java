package com.carebridge.backend.nearbycare.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondSupportRequest {

    @NotNull(message = "action is required")
    private String action; // ACCEPT, DECLINE, STOP

    private String note;
}
