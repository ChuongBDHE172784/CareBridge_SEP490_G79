package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadCredentialRequest {

    @NotNull(message = "Credential type is required")
    @NotEmpty(message = "Credential type cannot be empty")
    private String credentialType;

    @NotNull(message = "File is required")
    private MultipartFile file;

    private String issuingAuthority;

    private String credentialNumber;

    private java.time.LocalDate issueDate;

    private java.time.LocalDate expiryDate;
}
