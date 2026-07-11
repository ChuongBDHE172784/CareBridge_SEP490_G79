package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.ServiceApprovalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubmitServiceListingResponse(UUID serviceId, UUID partnerId, String serviceName,
        String description, BigDecimal priceFrom, String currency, String bookingUrl,
        ServiceApprovalStatus approvalStatus, Instant createdAt) {}
