package com.carebridge.backend.file.policy;

import com.carebridge.backend.file.entity.UploadedFile;

import java.util.Collection;
import java.util.UUID;

public interface FileAccessPolicy {

    /**
     * Asserts that callerId is permitted to view the given file.
     * Owner OR care-group-sharing-chain (ACCEPTED) OR admin role is required.
     * Throws AccessDeniedBusinessException (FILE-403) if the check fails.
     */
    void assertViewable(UploadedFile file, UUID callerId, Collection<String> callerAuthorities);
}
