package com.carebridge.backend.nearbycare.service.impl;

import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.nearbycare.dto.request.CreateNearbySupportRequest;
import com.carebridge.backend.nearbycare.dto.request.RespondSupportRequest;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportRequestResponse;
import com.carebridge.backend.nearbycare.dto.response.NearbySupportResponseResponse;
import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import com.carebridge.backend.nearbycare.mapper.NearbySupportMapper;
import com.carebridge.backend.nearbycare.repository.NearbySupportRequestRepository;
import com.carebridge.backend.nearbycare.repository.NearbySupportResponseRepository;
import com.carebridge.backend.nearbycare.service.INearbySupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NearbySupportServiceImpl implements INearbySupportService {

    private final NearbySupportRequestRepository requestRepository;
    private final NearbySupportResponseRepository responseRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final NearbySupportMapper mapper;

    @Override
    public NearbySupportRequest createRequest(UUID requesterUserId, CreateNearbySupportRequest request) {
        var entity = mapper.toEntity(requesterUserId, request);
        var saved = requestRepository.save(entity);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbySupportRequest> getMyRequests(UUID requesterUserId) {
        return requestRepository.findByRequesterUserId(requesterUserId);
    }

    @Override
    public NearbySupportRequest cancelRequest(UUID requestId, UUID requesterUserId) {
        var request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "NEARBY-001", "Support request not found"));

        if (!request.getRequesterUserId().equals(requesterUserId)) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "NEARBY-002", "Not authorized to cancel this request");
        }

        if (request.getStatus() != NearbySupportRequest.SupportStatus.OPEN) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "NEARBY-003", "Request is not open");
        }

        request.setStatus(NearbySupportRequest.SupportStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        return requestRepository.save(request);
    }

    @Override
    public NearbySupportResponse respondToRequest(UUID requestId, UUID expertProfileId, RespondSupportRequest request) {
        var supportRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "NEARBY-001", "Support request not found"));

        if (supportRequest.getStatus() != NearbySupportRequest.SupportStatus.OPEN) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "NEARBY-004", "Request is no longer open");
        }

        var profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ExpertException(HttpStatus.FORBIDDEN, "NEARBY-005", "Only verified experts can respond");
        }

        var action = NearbySupportResponse.ResponseAction.valueOf(request.getAction());
        var response = NearbySupportResponse.builder()
                .requestId(requestId)
                .expertProfileId(expertProfileId)
                .action(action)
                .note(request.getNote())
                .build();

        var saved = responseRepository.save(response);

        if (action == NearbySupportResponse.ResponseAction.ACCEPT) {
            supportRequest.setStatus(NearbySupportRequest.SupportStatus.ACCEPTED);
            supportRequest.setRespondedAt(LocalDateTime.now());
            requestRepository.save(supportRequest);
        }

        return saved;
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
