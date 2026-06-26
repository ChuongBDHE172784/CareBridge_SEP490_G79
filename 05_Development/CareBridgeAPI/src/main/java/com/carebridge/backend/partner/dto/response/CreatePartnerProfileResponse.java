package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerProfileResponse {

    private UUID id;
    private String name;
    private OrganizationType type;
    private OrganizationStatus status;
    private Instant createdAt;
}
