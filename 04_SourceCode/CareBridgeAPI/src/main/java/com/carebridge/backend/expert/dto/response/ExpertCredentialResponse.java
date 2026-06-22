package com.carebridge.backend.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertCredentialResponse {

    private UUID id;

    private String credentialType;

    private String fileName;

    private String fileUrl;

    private String issuingAuthority;

    private String verificationStatus;

    private UUID verifiedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant verifiedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
