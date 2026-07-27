package com.carebridge.backend.nearbycare.service.impl;

import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.nearbycare.dto.request.CreateNearbySupportRequest;
import com.carebridge.backend.nearbycare.dto.request.RespondSupportRequest;
import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import com.carebridge.backend.nearbycare.repository.NearbySupportRequestRepository;
import com.carebridge.backend.nearbycare.repository.NearbySupportResponseRepository;
import com.carebridge.backend.nearbycare.service.INearbySupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Nearby peer-to-peer support is DISABLED for safety and privacy (approved
 * 53→48 consolidation). The canonical schema keeps only an empty, write-
 * rejecting compatibility view. Reads return empty collections so existing
 * clients degrade gracefully; writes fail fast with an explicit contract
 * instead of surfacing a database trigger error — and never persist any
 * location data.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NearbySupportServiceImpl implements INearbySupportService {

    private static final String FEATURE_DISABLED_CODE = "NEARBY-410";
    private static final String FEATURE_DISABLED_MESSAGE =
            "Nearby peer support has been disabled for safety and privacy reasons";

    private final NearbySupportRequestRepository requestRepository;
    private final NearbySupportResponseRepository responseRepository;

    private static ExpertException featureDisabled() {
        return new ExpertException(HttpStatus.GONE, FEATURE_DISABLED_CODE, FEATURE_DISABLED_MESSAGE);
    }

    @Override
    public NearbySupportRequest createRequest(UUID requesterUserId, CreateNearbySupportRequest request) {
        throw featureDisabled();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbySupportRequest> getMyRequests(UUID requesterUserId) {
        return requestRepository.findByRequesterUserId(requesterUserId);
    }

    @Override
    public NearbySupportRequest cancelRequest(UUID requestId, UUID requesterUserId) {
        throw featureDisabled();
    }

    @Override
    public NearbySupportResponse respondToRequest(UUID requestId, UUID expertProfileId, RespondSupportRequest request) {
        throw featureDisabled();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbySupportRequest> getOpenRequests() {
        return requestRepository.findByStatus(NearbySupportRequest.SupportStatus.OPEN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbySupportResponse> getMyResponses(UUID expertProfileId) {
        return responseRepository.findByExpertProfileId(expertProfileId);
    }
}
