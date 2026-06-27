package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.IJourneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class JourneyServiceImpl implements IJourneyService {

    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;

    @Override
    public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId) {
        // C1: no duplicate active journey of same type
        boolean exists = journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                callerId, request.getJourneyType(), JourneyStatus.ACTIVE);
        if (exists) {
            throw new BusinessException(HttpStatus.CONFLICT, "JOURNEY-002",
                    "An active journey of type " + request.getJourneyType() + " already exists");
        }

        // C4: accountId from JWT (callerId), never from request body
        MotherJourney journey = MotherJourney.builder()
                .ownerUserId(callerId)
                .journeyType(request.getJourneyType())
                .startDate(request.getStartDate())
                .estimatedDueDate(request.getEstimatedDueDate())
                .notes(request.getNotes())
                .status(JourneyStatus.ACTIVE)
                .build();

        MotherJourney saved = journeyRepository.save(journey);

        // C3: emit audit event
        auditService.log(AuditAction.JOURNEY_CREATED, callerId,
                "MotherJourney", saved.getId().toString(), "created");

        return CreateJourneyResponse.builder()
                .id(saved.getId())
                .journeyType(saved.getJourneyType().name())
                .status(saved.getStatus().name())
                .startDate(saved.getStartDate())
                .estimatedDueDate(saved.getEstimatedDueDate())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
