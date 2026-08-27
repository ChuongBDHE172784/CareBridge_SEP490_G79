package com.carebridge.backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class UploadFileResponse {

public UploadFileResponse() {}

private UUID fileId;
private String originalName;
private String mimeType;
private long fileSizeBytes;
private String presignedUrl;
private Instant createdAt;
}
