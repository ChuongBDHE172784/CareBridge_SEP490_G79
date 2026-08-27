package com.carebridge.backend.file.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileAccessPolicyImpl implements FileAccessPolicy {

    private static final Set<String> ADMIN_ROLES =
            Set.of("ROLE_SYSTEM_ADMIN", "ROLE_MODERATOR", "ROLE_CONTENT_ADMIN");

    private final HealthRecordFileRepository healthRecordFileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;

    @Override
    public void assertViewable(UploadedFile file, UUID callerId, Collection<String> callerAuthorities) {
        // Rule 1 (ADR-FILE-005): Owner always has access
        if (file.getOwnerUserId().equals(callerId)) {
            return;
        }

        // Thoả thuận hợp tác chuyên gia mang điều khoản thù lao — thu hẹp khỏi Rule 3.
        // Moderator/Content admin cần soi bằng cấp y khoa, không cần biết điều khoản hợp tác.
        if (file.getPurpose() == FilePurpose.EXPERT_CONTRACT) {
            if (callerAuthorities.contains("ROLE_SYSTEM_ADMIN")) {
                return;
            }
            throw new AccessDeniedBusinessException(
                    "Access denied to expert contract " + file.getId());
        }

        // Rule 3 (ADR-FILE-005): Admin roles bypass scope check
        if (callerAuthorities.stream().anyMatch(ADMIN_ROLES::contains)) {
            return;
        }

        // Rule 2 (ADR-FILE-005): ACCEPTED care-group member sharing the linked health record / owner / baby
        boolean sharedAccess = healthRecordFileRepository.findByFileId(file.getId()).stream()
                .flatMap(link -> healthRecordRepository.findById(link.getHealthRecordId()).stream())
                .anyMatch(hr -> {
                    if (hr.getOwnerUserId().equals(callerId)) {
                        return true;
                    }
                    var callerMemberships = careGroupMemberRepository
                            .findByUserIdAndInviteStatus(callerId, InviteStatus.ACCEPTED);
                    for (var m : callerMemberships) {
                        var groupOpt = careGroupRepository.findById(m.getCareGroupId());
                        if (groupOpt.isPresent() && groupOpt.get().getOwnerUserId().equals(hr.getOwnerUserId())) {
                            return true;
                        }
                    }
                    if (hr.getBabyId() != null) {
                        return careGroupRepository.findByLinkedBabyProfileId(hr.getBabyId()).stream()
                                .anyMatch(cg -> careGroupMemberRepository
                                        .existsByCareGroupIdAndUserIdAndInviteStatus(cg.getId(), callerId, InviteStatus.ACCEPTED));
                    }
                    return false;
                });

        if (sharedAccess) {
            return;
        }

        throw new AccessDeniedBusinessException("Access denied to file " + file.getId());
    }
}
