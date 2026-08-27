package com.carebridge.backend.file.policy;

import com.carebridge.backend.file.entity.UploadedFile;

import java.util.UUID;

public interface FileDeletePolicy {

    /**
     * Asserts that callerId is permitted to delete the given file.
     * Strict owner-only; throws AccessDeniedBusinessException (FILE-403) for non-owners.
     * Throws BusinessException (FILE-409) if the attachment is linked to a health record.
     */
    void assertDeletable(UploadedFile file, UUID callerId);
}
