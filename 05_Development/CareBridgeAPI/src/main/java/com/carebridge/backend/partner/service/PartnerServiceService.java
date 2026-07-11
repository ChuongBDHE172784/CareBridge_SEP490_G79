package com.carebridge.backend.partner.service;

import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import java.util.UUID;
import com.carebridge.backend.partner.dto.response.PartnerServiceListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartnerServiceService {
    SubmitServiceListingResponse submitService(SubmitServiceListingRequest request, UUID actorId);
    Page<PartnerServiceListItemResponse> getOwnServices(UUID actorId, Pageable pageable);
}
