package com.carebridge.backend.baby.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.ArchiveBabyProfileResponse;
import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.dto.UpdateBabyProfileRequest;
import com.carebridge.backend.baby.dto.UpdateBabyProfileResponse;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.RoundingMode;
import java.util.Map;
import com.carebridge.backend.baby.dto.LinkBabyJourneyRequest;
import com.carebridge.backend.baby.dto.LinkBabyJourneyResponse;
import com.carebridge.backend.baby.entity.BabyLinkOperation;
import com.carebridge.backend.baby.entity.BabyLinkSubmission;
import com.carebridge.backend.baby.policy.BabyJourneyLinkagePolicy;
import com.carebridge.backend.baby.repository.BabyLinkSubmissionRepository;
import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@Transactional
@RequiredArgsConstructor
public class BabyServiceImpl implements IBabyService {

    private final BabyProfileRepository babyRepository;
    private final BabyAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final BabyJourneyLinkagePolicy linkagePolicy;
    private final BabyLinkSubmissionRepository linkSubmissionRepository;
    private final BabyLinkRejectionAuditService rejectionAuditService;

    @Override
    public CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId) {
        if (request.getRelatedJourneyId() != null) {
            try {
                return createBabyWithLink(request, callerId);
            } catch (BusinessException ex) {
                rejectionAuditService.record(callerId, request.getRelatedJourneyId(), ex.getCode());
                throw ex;
            }
        }
        // C4: accountId from JWT (callerId)
        BabyProfile profile = BabyProfile.builder()
                .ownerUserId(callerId)
                .nickname(request.getNickname())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .birthWeightKg(request.getBirthWeightKg())
                .birthLengthCm(request.getBirthLengthCm())
                .relatedJourneyId(request.getRelatedJourneyId())
                .build();

        BabyProfile saved = babyRepository.save(profile);

        // C3: emit audit event
        auditService.log(AuditAction.BABY_PROFILE_CREATED, callerId,
                "BabyProfile", saved.getId().toString(), "created");

        return CreateBabyProfileResponse.builder()
                .id(saved.getId())
                .nickname(saved.getNickname())
                .birthDate(saved.getBirthDate())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .birthWeightKg(saved.getBirthWeightKg())
                .birthLengthCm(saved.getBirthLengthCm())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .relatedJourneyId(saved.getRelatedJourneyId())
                .build();
    }

    private CreateBabyProfileResponse createBabyWithLink(CreateBabyProfileRequest request, UUID callerId) {
        linkagePolicy.requireEligibleJourney(request.getRelatedJourneyId(), callerId);
        String intent = createIntent(request);
        acquireSubmissionLock(callerId, BabyLinkOperation.CREATE_WITH_LINK, request.getSubmissionId());
        var prior = linkSubmissionRepository.findForUpdate(callerId, BabyLinkOperation.CREATE_WITH_LINK, request.getSubmissionId());
        if (prior.isPresent()) {
            ensureSameIntent(prior.get(), intent);
            return toCreateResponse(babyRepository.findByIdAndOwnerUserId(prior.get().getBabyId(), callerId)
                    .orElseThrow(BabyJourneyLinkagePolicy::notEligible));
        }
        BabyProfile saved = babyRepository.save(BabyProfile.builder()
                .ownerUserId(callerId).nickname(request.getNickname().trim()).birthDate(request.getBirthDate())
                .gender(request.getGender()).birthWeightKg(request.getBirthWeightKg()).birthLengthCm(request.getBirthLengthCm())
                .relatedJourneyId(request.getRelatedJourneyId()).build());
        linkSubmissionRepository.save(BabyLinkSubmission.builder().ownerUserId(callerId)
                .operationType(BabyLinkOperation.CREATE_WITH_LINK).submissionId(request.getSubmissionId())
                .semanticIntent(intent).babyId(saved.getId()).journeyId(request.getRelatedJourneyId()).build());
        auditService.log(AuditAction.BABY_JOURNEY_LINK_ACCEPTED, callerId, "BabyJourneyLink", saved.getId().toString(),
                Map.of("journeyId", request.getRelatedJourneyId(), "operation", "CREATE_WITH_LINK"));
        return toCreateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BabyProfileDetailResponse> listBabyProfiles(UUID callerId) {
        return babyRepository
                .findByOwnerUserIdAndStatusOrderByCreatedAtAsc(callerId, BabyProfileStatus.ACTIVE)
                .stream()
                .map(p -> BabyProfileDetailResponse.builder()
                        .id(p.getId())
                        .nickname(p.getNickname())
                        .birthDate(p.getBirthDate())
                        .gender(p.getGender() != null ? p.getGender().name() : null)
                        .birthWeightKg(p.getBirthWeightKg())
                        .birthLengthCm(p.getBirthLengthCm())
                        .status(p.getStatus().name())
                        .active(Boolean.TRUE.equals(p.getActive()))
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .relatedJourneyId(p.getRelatedJourneyId())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID callerId) {
        BabyProfile profile = babyRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-001",
                        "Baby profile not found: " + profileId));

        // C1: BabyAccessPolicy — ownership OR ACCEPTED care group member
        if (!accessPolicy.canView(profile, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-003",
                    "Access denied to baby profile");
        }

        // C2: ARCHIVED still returns 200 (BR-BABY-011)
        return BabyProfileDetailResponse.builder()
                .id(profile.getId())
                .nickname(profile.getNickname())
                .birthDate(profile.getBirthDate())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .birthWeightKg(profile.getBirthWeightKg())
                .birthLengthCm(profile.getBirthLengthCm())
                .status(profile.getStatus().name())
                .active(Boolean.TRUE.equals(profile.getActive()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .relatedJourneyId(profile.getRelatedJourneyId())
                .build();
    }

    @Override
    public BabyProfileDetailResponse switchActiveBabyProfile(UUID babyId, UUID callerId) {
        BabyProfile profile = babyRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-001",
                        "Baby profile not found: " + babyId));

        if (!accessPolicy.canManage(profile, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-003",
                    "Access denied to baby profile");
        }
        if (profile.getStatus() == BabyProfileStatus.ARCHIVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-004",
                    "Cannot activate archived baby profile");
        }

        babyRepository.setActiveBaby(callerId, babyId);
        profile.setActive(true);
        BabyProfile saved = babyRepository.save(profile);

        auditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, callerId,
                "BabyProfile", saved.getId().toString(), "active");

        return BabyProfileDetailResponse.builder()
                .id(saved.getId())
                .nickname(saved.getNickname())
                .birthDate(saved.getBirthDate())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .birthWeightKg(saved.getBirthWeightKg())
                .birthLengthCm(saved.getBirthLengthCm())
                .status(saved.getStatus().name())
                .active(Boolean.TRUE.equals(saved.getActive()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .relatedJourneyId(saved.getRelatedJourneyId())
                .build();
    }

    @Override
    public UpdateBabyProfileResponse updateBabyProfile(UUID babyId, UpdateBabyProfileRequest request, UUID callerId) {
        // C1: load profile
        BabyProfile profile = babyRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-010",
                        "Baby profile not found: " + babyId));

        // C1: ownership check
        if (!profile.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-011",
                    "Access denied to baby profile");
        }

        // C2: ACTIVE only
        if (profile.getStatus() != BabyProfileStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-012",
                    "Cannot update archived baby profile");
        }

        // C3: apply non-null mutable fields
        if (request.getNickname() != null) profile.setNickname(request.getNickname());
        if (request.getBirthDate() != null) profile.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getBirthWeightKg() != null) profile.setBirthWeightKg(request.getBirthWeightKg());
        if (request.getBirthLengthCm() != null) profile.setBirthLengthCm(request.getBirthLengthCm());

        BabyProfile saved = babyRepository.save(profile);

        // C4: audit
        auditService.log(AuditAction.BABY_PROFILE_UPDATED, callerId,
                "BabyProfile", saved.getId().toString(), "updated");

        return UpdateBabyProfileResponse.builder()
                .babyId(saved.getId())
                .nickname(saved.getNickname())
                .birthDate(saved.getBirthDate())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .birthWeightKg(saved.getBirthWeightKg())
                .birthLengthCm(saved.getBirthLengthCm())
                .status(saved.getStatus().name())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    public ArchiveBabyProfileResponse archiveBabyProfile(UUID babyId, UUID callerId) {
        // C1: load profile
        BabyProfile profile = babyRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-020",
                        "Baby profile not found: " + babyId));

        // C1: ownership check
        if (!profile.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-021",
                    "Access denied to baby profile");
        }

        // C2: NOT idempotent — already archived → reject
        if (profile.getStatus() == BabyProfileStatus.ARCHIVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-022",
                    "Baby profile is already archived");
        }

        // C2: soft-archive only, linked data preserved
        profile.setStatus(BabyProfileStatus.ARCHIVED);
        BabyProfile saved = babyRepository.save(profile);

        // C4: audit
        auditService.log(AuditAction.BABY_PROFILE_ARCHIVED, callerId,
                "BabyProfile", saved.getId().toString(), "archived");

        return ArchiveBabyProfileResponse.builder()
                .babyId(saved.getId())
                .status(saved.getStatus().name())
                .archivedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    public LinkBabyJourneyResponse linkExistingBaby(UUID babyId, LinkBabyJourneyRequest request, UUID callerId) {
        try {
            linkagePolicy.requireEligibleJourney(request.getRelatedJourneyId(), callerId);
            BabyProfile baby = babyRepository.findOwnedByIdForUpdate(babyId, callerId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LINK_RESOURCE_NOT_FOUND", "Resource not found"));
            if (baby.getStatus() != BabyProfileStatus.ACTIVE) throw BabyJourneyLinkagePolicy.notEligible();
            String intent = babyId + "|" + request.getRelatedJourneyId();
            acquireSubmissionLock(callerId, BabyLinkOperation.LINK_EXISTING, request.getSubmissionId());
            var prior = linkSubmissionRepository.findForUpdate(callerId, BabyLinkOperation.LINK_EXISTING, request.getSubmissionId());
            if (prior.isPresent()) {
                ensureSameIntent(prior.get(), intent);
                return LinkBabyJourneyResponse.builder().babyId(prior.get().getBabyId()).relatedJourneyId(prior.get().getJourneyId()).build();
            }
            if (baby.getRelatedJourneyId() != null && !baby.getRelatedJourneyId().equals(request.getRelatedJourneyId())) {
                throw new BusinessException(HttpStatus.CONFLICT, "BABY_ALREADY_LINKED", "Baby is already linked");
            }
            boolean newlyLinked = baby.getRelatedJourneyId() == null;
            baby.setRelatedJourneyId(request.getRelatedJourneyId());
            babyRepository.save(baby);
            linkSubmissionRepository.save(BabyLinkSubmission.builder().ownerUserId(callerId)
                    .operationType(BabyLinkOperation.LINK_EXISTING).submissionId(request.getSubmissionId())
                    .semanticIntent(intent).babyId(babyId).journeyId(request.getRelatedJourneyId()).build());
            if (newlyLinked) auditService.log(AuditAction.BABY_JOURNEY_LINK_ACCEPTED, callerId, "BabyJourneyLink", babyId.toString(),
                    Map.of("journeyId", request.getRelatedJourneyId(), "operation", "LINK_EXISTING"));
            return LinkBabyJourneyResponse.builder().babyId(babyId).relatedJourneyId(request.getRelatedJourneyId()).build();
        } catch (BusinessException ex) {
            rejectionAuditService.record(callerId, babyId, ex.getCode());
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly=true)
    public PaginatedResponse<BabyProfileDetailResponse> listJourneyBabies(UUID journeyId, int page, int size, UUID callerId) {
        linkagePolicy.requireEligibleJourneyForRead(journeyId, callerId);
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Order.asc("createdAt"),Sort.Order.asc("id")));
        return PaginatedResponse.of(babyRepository.findByOwnerUserIdAndRelatedJourneyIdAndStatus(callerId, journeyId, BabyProfileStatus.ACTIVE, pageable)
                .map(this::toDetailResponse));
    }

    private void acquireSubmissionLock(UUID owner, BabyLinkOperation operation, UUID submission) {
        linkSubmissionRepository.acquireTransactionLock(owner+":"+operation+":"+submission);
    }

    private void ensureSameIntent(BabyLinkSubmission prior, String intent) {
        if (!prior.getSemanticIntent().equals(intent))
            throw new BusinessException(HttpStatus.CONFLICT, "LINK_SUBMISSION_CONFLICT", "Submission conflicts with an earlier request");
    }

    private String createIntent(CreateBabyProfileRequest r) {
        return String.join("|", r.getRelatedJourneyId().toString(), r.getNickname().trim(), r.getBirthDate().toString(),
                String.valueOf(r.getGender()), decimal(r.getBirthWeightKg(),2), decimal(r.getBirthLengthCm(),1));
    }

    private String decimal(java.math.BigDecimal value, int scale) {
        return value == null ? "" : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private CreateBabyProfileResponse toCreateResponse(BabyProfile p) {
        return CreateBabyProfileResponse.builder().id(p.getId()).nickname(p.getNickname()).birthDate(p.getBirthDate())
                .gender(p.getGender()==null?null:p.getGender().name()).birthWeightKg(p.getBirthWeightKg())
                .birthLengthCm(p.getBirthLengthCm()).status(p.getStatus().name()).createdAt(p.getCreatedAt())
                .relatedJourneyId(p.getRelatedJourneyId()).build();
    }

    private BabyProfileDetailResponse toDetailResponse(BabyProfile p) {
        return BabyProfileDetailResponse.builder().id(p.getId()).nickname(p.getNickname()).birthDate(p.getBirthDate())
                .gender(p.getGender()==null?null:p.getGender().name()).birthWeightKg(p.getBirthWeightKg())
                .birthLengthCm(p.getBirthLengthCm()).status(p.getStatus().name()).active(Boolean.TRUE.equals(p.getActive()))
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).relatedJourneyId(p.getRelatedJourneyId()).build();
    }
}
