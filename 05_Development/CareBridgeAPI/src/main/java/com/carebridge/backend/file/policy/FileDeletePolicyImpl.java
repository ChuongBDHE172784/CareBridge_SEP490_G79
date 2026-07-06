package com.carebridge.backend.file.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.UploadedFile;
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

        // Rule 2 (ADR-FILE-007): Binding guard — no deletion while referenced by health_record_files
        if (!healthRecordFileRepository.findByFileId(file.getId()).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-409",
                    "File is bound to a health record and cannot be deleted");
        }
    }
}
