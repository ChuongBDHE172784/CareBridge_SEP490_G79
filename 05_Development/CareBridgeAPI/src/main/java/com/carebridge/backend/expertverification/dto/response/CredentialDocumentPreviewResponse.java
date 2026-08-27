package com.carebridge.backend.expertverification.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CredentialDocumentPreviewResponse {
    UUID credentialId;
    String fileName;
    String mimeType;
    long fileSizeBytes;
    String content;
    boolean truncated;
}
