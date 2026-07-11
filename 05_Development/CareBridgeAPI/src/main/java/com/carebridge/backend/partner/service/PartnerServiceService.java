package com.carebridge.backend.partner.service;

import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import java.util.UUID;

public interface PartnerServiceService {
    SubmitServiceListingResponse submitService(SubmitServiceListingRequest request, UUID actorId);
}
