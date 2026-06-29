package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FileAttachmentDto {

    private UUID fileId;
    private String originalName;
    private String mimeType;
    private int displayOrder;
    private String presignedUrl;
}
