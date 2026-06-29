package com.carebridge.backend.expert.dto.request;

import com.carebridge.backend.expert.enums.CredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Request DTO for uploading a verification document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVerificationDocumentRequest {

    /**
     * Type of verification document.
     */
    @NotNull(message = "Credential type is required")
    private CredentialType credentialType;

    /**
     * Document number (license number, certificate ID, etc.).
     */
    @NotBlank(message = "Credential number is required")
    private String credentialNumber;

    /**
     * Issuing authority.
     */
    private String issuer;

    /**
     * Issue date.
     */
    private java.time.Instant issuedDate;

    /**
     * Expiry date.
     */
    private java.time.Instant expiryDate;

    /**
     * The document file (multipart).
     * Max size: 5MB.
     */
    @NotNull(message = "Document file is required")
    private InputStream file;

    /**
     * Original filename.
     */
    @NotBlank(message = "Filename is required")
    private String filename;

    /**
     * Document credential type enumeration.
     * LICENSE, CERTIFICATE, ID_CARD, PASSPORT, MEDICAL_DEGREE.
     */
    public enum CredentialType {
        LICENSE,
        CERTIFICATE,
        ID_CARD,
        PASSPORT,
        MEDICAL_DEGREE,
        OTHER
    }
}
