package com.carebridge.backend.file.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileDeletePolicyImpl implements FileDeletePolicy {

    private final HealthRecordFileRepository healthRecordFileRepository;

    @Override
    public void assertDeletable(UploadedFile file, UUID callerId) {
        // Rule 1 (ADR-FILE-007): Strict owner-only — no admin or family bypass
        if (!file.getOwnerUserId().equals(callerId)) {
            throw new AccessDeniedBusinessException("Only the file owner can delete it");
        }

        // Thông tư 33/2025/TT-BYT: hồ sơ tư vấn lưu tối thiểu 10 năm. Chuyên gia đứng tên owner
        // của chính bản thoả thuận mình ký, nên nếu không chặn ở đây thì Rule 1 cho phép họ xoá
        // mất bằng chứng đồng thuận.
        if (file.getPurpose() == FilePurpose.EXPERT_CONTRACT) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-409",
                    "Thoả thuận hợp tác phải được lưu trữ theo quy định và không thể xoá");
        }

        // Rule 2 (ADR-FILE-007): no deletion while linked to a health record.
        if (!healthRecordFileRepository.findByFileId(file.getId()).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-409",
                    "File is bound to a health record and cannot be deleted");
        }
    }
}
