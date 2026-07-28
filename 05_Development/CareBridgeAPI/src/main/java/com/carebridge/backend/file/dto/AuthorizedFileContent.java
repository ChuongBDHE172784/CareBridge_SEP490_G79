package com.carebridge.backend.file.dto;

import java.util.UUID;

/**
 * Internal-only authorized file payload. Raw bytes are never serialized by a controller.
 */
public record AuthorizedFileContent(
        UUID fileId,
        String originalName,
        String mimeType,
        long fileSizeBytes,
        byte[] bytes) {
}
