package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AddMilestoneRequest;
import com.carebridge.backend.carejourney.dto.MilestoneResponse;
import com.carebridge.backend.carejourney.entity.DevelopmentMilestone;
import com.carebridge.backend.carejourney.repository.DevelopmentMilestoneRepository;
import com.carebridge.backend.carejourney.service.IMilestoneService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MilestoneServiceImpl implements IMilestoneService {

    // ADR-BABY-007-001: duplicate milestone types allowed — no uniqueness check
    private static final Set<String> VALID_MILESTONE_TYPES = Set.of(
            "ROLLING", "CRAWLING", "WALKING", "SPEAKING",
            "TEETHING", "WEANING", "FIRST_SMILE", "SITTING", "STANDING"
    );

    private final DevelopmentMilestoneRepository milestoneRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final AuditService auditService;

    @Override
    public MilestoneResponse addMilestone(UUID userId, UUID babyId, AddMilestoneRequest request) {
        // C1 + BABY-060: find baby
        BabyProfile baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby not found: " + babyId));

        // C1 + BABY-061: ownership check (BR-RBAC)
        if (!baby.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedBusinessException("Baby not owned by user");
        }

        // C2 + BABY-062: baby must be ACTIVE
        if (BabyProfileStatus.ARCHIVED.equals(baby.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-062", "Baby is archived");
        }

        // BABY-063: validate milestoneType (ADR-BABY-007-001 — duplicates OK, invalid types rejected)
        if (!VALID_MILESTONE_TYPES.contains(request.getMilestoneType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-063",
                    "Invalid milestone type. Valid types: " + VALID_MILESTONE_TYPES);
        }

        // C3 + BABY-064: achievedDate must not be in the future (ADR-BABY-007-002)
        if (request.getAchievedDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-064",
                    "Achieved date cannot be in the future");
        }

        // C4: recordedBy = JWT userId (not from request body)
        // sourceType defaults to "MANUAL" when not provided
        DevelopmentMilestone milestone = DevelopmentMilestone.builder()
                .babyId(babyId)
                .milestoneType(request.getMilestoneType())
                .achievedDate(request.getAchievedDate())
                .note(request.getNote())
                .sourceType(request.getSourceType() != null ? request.getSourceType() : "MANUAL")
                .recordedBy(userId)
                .build();

        DevelopmentMilestone saved = milestoneRepository.save(milestone);

        // C5: emit MILESTONE_RECORDED audit event (BR-SAFETY / PDPA)
        auditService.log(AuditAction.MILESTONE_RECORDED, userId,
                "DevelopmentMilestone", saved.getMilestoneId().toString(), request.getMilestoneType());

        return toResponse(saved);
    }

    private MilestoneResponse toResponse(DevelopmentMilestone milestone) {
        return MilestoneResponse.builder()
                .milestoneId(milestone.getMilestoneId())
                .babyId(milestone.getBabyId())
                .milestoneType(milestone.getMilestoneType())
                .achievedDate(milestone.getAchievedDate())
                .note(milestone.getNote())
                .sourceType(milestone.getSourceType())
                .recordedBy(milestone.getRecordedBy())
                .createdAt(milestone.getCreatedAt())
                .build();
    }
}
