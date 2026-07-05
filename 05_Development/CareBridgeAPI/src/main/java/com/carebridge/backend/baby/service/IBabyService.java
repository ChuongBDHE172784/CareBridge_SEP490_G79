package com.carebridge.backend.baby.service;

import com.carebridge.backend.baby.dto.ArchiveBabyProfileResponse;
import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.dto.UpdateBabyProfileRequest;
import com.carebridge.backend.baby.dto.UpdateBabyProfileResponse;

import java.util.List;
import java.util.UUID;

public interface IBabyService {

    CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId);

    /** Returns all ACTIVE baby profiles owned by the caller, ordered by creation date. */
    List<BabyProfileDetailResponse> listBabyProfiles(UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (BABY-003/403) if no access */
    BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID callerId);

    /** UC32: Update mutable fields of an ACTIVE baby profile.
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-010/404, BABY-011/403, BABY-012/400)
     */
    UpdateBabyProfileResponse updateBabyProfile(UUID babyId, UpdateBabyProfileRequest request, UUID callerId);

    /** UC33: Soft-archive a baby profile. Linked data is preserved.
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-020/404, BABY-021/403, BABY-022/400)
     */
    ArchiveBabyProfileResponse archiveBabyProfile(UUID babyId, UUID callerId);
}
