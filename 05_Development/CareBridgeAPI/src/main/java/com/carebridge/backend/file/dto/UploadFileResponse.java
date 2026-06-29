package com.carebridge.backend.file.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UploadFileResponse {

    private UUID fileId;
    private String originalName;
    private String mimeType;
    private long fileSizeBytes;
    private String presignedUrl;
    private Instant createdAt;
}
