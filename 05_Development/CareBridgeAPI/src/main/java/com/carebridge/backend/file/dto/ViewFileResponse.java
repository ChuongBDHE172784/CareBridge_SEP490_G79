package com.carebridge.backend.file.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ViewFileResponse {

    private UUID fileId;
    private String originalName;
    private String mimeType;
    private long fileSizeBytes;
    private String presignedUrl;
    private String status;
    private Instant createdAt;
}
