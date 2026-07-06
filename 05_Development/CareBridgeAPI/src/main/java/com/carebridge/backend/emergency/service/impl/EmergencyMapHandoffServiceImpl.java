package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.emergency.dto.request.CreateEmergencyHandoffRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyHandoffResponse;
import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import com.carebridge.backend.map.mapper.EmergencyMapHandoffMapper;
import com.carebridge.backend.emergency.repository.EmergencyMapHandoffRepository;
import com.carebridge.backend.emergency.service.IEmergencyMapHandoffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EmergencyMapHandoffServiceImpl implements IEmergencyMapHandoffService {

    private final EmergencyMapHandoffRepository handoffRepository;
    private final EmergencyMapHandoffMapper handoffMapper;

    @Override
    public EmergencyHandoffResponse createHandoff(UUID userId, CreateEmergencyHandoffRequest request) {
        // PDPA: location only included when triage consent allows
        var handoff = handoffMapper.toEntity(userId, request);

        // Auto-accept if risk level is RED (emergency protocol)
        if ("RED".equalsIgnoreCase(request.getRiskLevel())) {
            handoff.setStatus(HandoffStatus.ACCEPTED);
        }
        var saved = handoffRepository.save(handoff);
        return handoffMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyHandoffResponse getHandoff(UUID handoffId) {
        return handoffRepository.findById(handoffId)
                .map(handoffMapper::toResponse)
                .orElseThrow(() -> new com.carebridge.backend.emergency.exception.EmergencyException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "EMER-001", "Emergency handoff not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyHandoffResponse> getMyHandoffs(UUID userId) {
        return handoffRepository.findByUserId(userId).stream()
                .map(handoffMapper::toResponse)
                .collect(Collectors.toList());
    }
}
