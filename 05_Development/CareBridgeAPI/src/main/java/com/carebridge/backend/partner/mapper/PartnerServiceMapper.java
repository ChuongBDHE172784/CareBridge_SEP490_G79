package com.carebridge.backend.partner.mapper;

import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import com.carebridge.backend.partner.entity.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PartnerServiceMapper {
    public PartnerService toEntity(SubmitServiceListingRequest request, UUID partnerId) {
        return PartnerService.builder().partnerId(partnerId).serviceName(request.serviceName())
                .description(request.description()).priceFrom(request.priceFrom())
                .currency(request.currency() == null || request.currency().isBlank() ? "VND" : request.currency())
                .bookingUrl(request.bookingUrl()).approvalStatus(ServiceApprovalStatus.PENDING).build();
    }
    public SubmitServiceListingResponse toResponse(PartnerService entity) {
        return new SubmitServiceListingResponse(entity.getId(),entity.getPartnerId(),entity.getServiceName(),
                entity.getDescription(),entity.getPriceFrom(),entity.getCurrency(),entity.getBookingUrl(),
                entity.getApprovalStatus(),entity.getCreatedAt());
    }
}
