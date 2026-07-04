package com.carebridge.backend.nearbycare.service;

import com.carebridge.backend.nearbycare.dto.request.CreateNearbySupportRequest;
import com.carebridge.backend.nearbycare.dto.request.RespondSupportRequest;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportRequestResponse;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportResponseResponse;
import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import java.util.List;
import java.util.UUID;

public interface INearbySupportService {

    NearbySupportRequest createRequest(UUID requesterUserId, CreateNearbySupportRequest request);

    List<NearbySupportRequest> getMyRequests(UUID requesterUserId);

    NearbySupportRequest cancelRequest(UUID requestId, UUID requesterUserId);

    NearbySupportResponse respondToRequest(UUID requestId, UUID expertProfileId, RespondSupportRequest request);

    List<NearbySupportRequest> getOpenRequests();

    List<NearbySupportResponse> getMyResponses(UUID expertProfileId);
}
