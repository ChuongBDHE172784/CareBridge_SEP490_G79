package com.carebridge.backend.consultation.dto.request;

import com.carebridge.backend.consultation.dto.request.validation.ValidPreferredWindow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidPreferredWindow
public class CreateConsultationRequestRequest {

    @NotNull
    private UUID clientRequestId;

    @NotNull
    private UUID expertProfileId;

    @NotBlank
    @Size(max = 200)
    private String topic;

    @NotBlank
    @Size(max = 2000)
    private String description;

    private Instant preferredWindowStart;
    private Instant preferredWindowEnd;
}
