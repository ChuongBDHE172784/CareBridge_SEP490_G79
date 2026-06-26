package com.carebridge.backend.consent.dto.response;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentGrantResponse {

    private Long id;
    private java.util.UUID userId;
    private ConsentDataType dataType;
    private ConsentPurpose purpose;
    private String recipient;
    private String scope;
    private Instant consentGivenAt;
    private Instant expiryAt;
    private Instant revokedAt;
}
