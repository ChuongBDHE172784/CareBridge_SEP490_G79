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
import com.carebridge.backend.baby.service.BabyBirthGrowthSynchronizer;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.vaccination.service.IVaccinationBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BabyServiceImpl implements IBabyService {

    private final BabyProfileRepository babyRepository;
    private final BabyAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final IVaccinationBookService vaccinationBookService;
    private final BabyBirthGrowthSynchronizer babyBirthGrowthSynchronizer;

    @Override
    public CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId) {
        // C4: accountId from JWT (callerId)
        BabyProfile profile = BabyProfile.builder()
                .ownerUserId(callerId)
                .nickname(request.getNickname())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .birthWeightKg(request.getBirthWeightKg())
                .birthLengthCm(request.getBirthLengthCm())
                .build();

        BabyProfile saved = babyRepository.save(profile);

        // Project the optional birth measurements into the canonical Growth history before
        // any success audit is emitted. The surrounding transaction also covers vaccination
        // initialization, so a failed projection cannot leave a partial baby profile behind.
        babyBirthGrowthSynchronizer.synchronize(saved);

        // MF-03: a newly registered baby gets the whole expected vaccination book straight
        // away, projected from the catalogue onto the birth date. Same transaction as the
        // profile insert so a baby never exists with a half-written book.
        vaccinationBookService.initializeBook(saved);

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
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BabyProfileDetailResponse> listBabyProfiles(UUID callerId) {
        UUID activeBabyId = babyRepository.findActiveBabyId(callerId).orElse(null);
        return babyRepository
                .findByOwnerUserIdAndStatusOrderByCreatedAtAsc(callerId, BabyProfileStatus.ACTIVE)
                .stream()
                .map(profile -> toDetailResponse(profile, activeBabyId))
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
        UUID activeBabyId = babyRepository.findActiveBabyId(callerId).orElse(null);
        return toDetailResponse(profile, activeBabyId);
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

        auditService.log(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED, callerId,
                "BabyProfile", profile.getId().toString(), "active");

        return toDetailResponse(profile, babyId);
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
        boolean birthDateChanged = request.getBirthDate() != null
                && !request.getBirthDate().equals(profile.getBirthDate());
        if (request.getNickname() != null) profile.setNickname(request.getNickname());
        if (request.getBirthDate() != null) profile.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getBirthWeightKg() != null) profile.setBirthWeightKg(request.getBirthWeightKg());
        if (request.getBirthLengthCm() != null) profile.setBirthLengthCm(request.getBirthLengthCm());

        BabyProfile saved = babyRepository.save(profile);

        // MF-03: expected dose dates are derived from the birth date, so a corrected birth
        // date has to re-project the still-SCHEDULED part of the book. Doses the mother has
        // already completed, postponed or deleted keep the dates she chose.
        if (birthDateChanged) {
            vaccinationBookService.realignBook(saved);
        }

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


    private BabyProfileDetailResponse toDetailResponse(BabyProfile p, UUID activeBabyId) {
        return BabyProfileDetailResponse.builder().id(p.getId()).nickname(p.getNickname()).birthDate(p.getBirthDate())
                .gender(p.getGender()==null?null:p.getGender().name()).birthWeightKg(p.getBirthWeightKg())
                .birthLengthCm(p.getBirthLengthCm()).status(p.getStatus().name()).active(p.getId().equals(activeBabyId))
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
