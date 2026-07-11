package com.carebridge.backend.partner.service;

import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.dto.request.UpdatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.UpdatePartnerProfileResponse;

public interface PartnerProfileService {

    /**
     * Creates a partner organization profile with status PENDING_APPROVAL.
     * actorId is always taken from the SecurityContext — never from the request body.
     */
    CreatePartnerProfileResponse createProfile(CreatePartnerProfileRequest request, java.util.UUID actorId);

    /** Updates the current partner representative's own organization profile. */
    UpdatePartnerProfileResponse updateProfile(UpdatePartnerProfileRequest request, java.util.UUID actorId);
}
