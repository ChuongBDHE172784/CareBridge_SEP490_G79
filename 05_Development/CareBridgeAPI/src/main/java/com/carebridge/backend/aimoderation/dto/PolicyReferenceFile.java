package com.carebridge.backend.aimoderation.dto;

import java.util.UUID;

public record PolicyReferenceFile(
        UUID fileId,
        String fileName,
        String fileUrl,
        Long fileSizeBytes
) {
}
