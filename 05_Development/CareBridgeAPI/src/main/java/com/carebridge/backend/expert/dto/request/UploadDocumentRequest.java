package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.entity.VerificationDocType;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UploadDocumentRequest(
    @NotNull(message = "EXP-006: file is required")
    MultipartFile file,

    @NotNull(message = "EXP-006: docType is required")
    VerificationDocType docType) {
}
