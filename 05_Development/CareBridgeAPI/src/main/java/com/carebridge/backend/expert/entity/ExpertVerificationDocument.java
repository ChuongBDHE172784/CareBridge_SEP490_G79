package com.carebridge.backend.expert.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expert_verification_documents")
public class ExpertVerificationDocument {

  @Id
  @GeneratedValue
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "expert_id", nullable = false)
  private UUID expertId;

  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false, length = 20)
  private VerificationDocType docType;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private VerificationDocStatus status;

  @Column(name = "reject_reason", columnDefinition = "TEXT")
  private String rejectReason;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @PrePersist
  void onCreate() {
    if (uploadedAt == null) {
      uploadedAt = Instant.now();
    }
    if (status == null) {
      status = VerificationDocStatus.PENDING_REVIEW;
    }
  }

  public ExpertVerificationDocument() {}

  public ExpertVerificationDocument(
      UUID expertId,
      VerificationDocType docType,
      String storageKey,
      String originalName,
      String mimeType,
      Long sizeBytes,
      VerificationDocStatus status) {
    this.expertId = expertId;
    this.docType = docType;
    this.storageKey = storageKey;
    this.originalName = originalName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.status = status;
  }

  public UUID getId() { return id; }
  public UUID getExpertId() { return expertId; }
  public VerificationDocType getDocType() { return docType; }
  public String getStorageKey() { return storageKey; }
  public String getOriginalName() { return originalName; }
  public String getMimeType() { return mimeType; }
  public Long getSizeBytes() { return sizeBytes; }
  public VerificationDocStatus getStatus() { return status; }
  public String getRejectReason() { return rejectReason; }
  public Instant getUploadedAt() { return uploadedAt; }
  public Instant getReviewedAt() { return reviewedAt; }
  public UUID getReviewedBy() { return reviewedBy; }

  public void setId(UUID id) { this.id = id; }
  public void setExpertId(UUID expertId) { this.expertId = expertId; }
  public void setDocType(VerificationDocType docType) { this.docType = docType; }
  public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
  public void setOriginalName(String originalName) { this.originalName = originalName; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
  public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
  public void setStatus(VerificationDocStatus status) { this.status = status; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
  public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
}
