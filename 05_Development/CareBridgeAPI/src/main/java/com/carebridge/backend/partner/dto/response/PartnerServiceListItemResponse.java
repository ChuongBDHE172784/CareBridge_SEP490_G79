package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.ServiceApprovalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartnerServiceListItemResponse(UUID id, String serviceName, String description,
        BigDecimal priceFrom, String currency, String bookingUrl, ServiceApprovalStatus approvalStatus,
        Instant createdAt) {}
