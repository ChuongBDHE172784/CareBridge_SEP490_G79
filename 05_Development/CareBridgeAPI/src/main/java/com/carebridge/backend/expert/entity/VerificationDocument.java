package com.carebridge.backend.expert.entity;

import com.carebridge.backend.expert.dto.request.UploadVerificationDocumentRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Verification document entity.
 * Stores uploaded verification documents for experts (licenses, certificates, etc.).
 *
 * ADR-EXP-003: Uses Firebase Storage with path-based access control.
 */
@Entity
@Table(name = "expert_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Long credentialId;

    /**
     * Foreign key to experts table.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Type of verification document.
     * LICENSE, CERTIFICATE, ID_CARD, PASSPORT, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 50)
    private UploadVerificationDocumentRequest.CredentialType credentialType;

    /**
     * Document number (e.g., license number, certificate ID).
     */
    @Column(name = "credential_number", length = 200)
    private String credentialNumber;

    /**
     * Issuing authority/organization.
     */
    @Column(name = "issuer", length = 200)
    private String issuer;

    /**
     * Date when the credential was issued.
     */
    @Column(name = "issued_date")
    private Instant issuedDate;

    /**
     * Date when the credential expires.
     */
    @Column(name = "expiry_date")
    private Instant expiryDate;

    /**
     * Firebase Storage path where the document is stored.
     * Format: /verification-documents/{expertId}/{credentialId}/{filename}
     */
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    /**
     * Original filename.
     */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Current review status of the document.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 50)
    private VerificationDocumentStatus reviewStatus;

    /**
     * Optional note from reviewer.
     */
    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    /**
     * Admin user who reviewed this document.
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /**
     * Timestamp when the document was reviewed.
     */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
