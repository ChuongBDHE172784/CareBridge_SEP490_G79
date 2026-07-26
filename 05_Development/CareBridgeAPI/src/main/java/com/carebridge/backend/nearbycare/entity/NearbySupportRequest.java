package com.carebridge.backend.nearbycare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nearby_support_interactions")
@org.hibernate.annotations.SQLRestriction("interaction_type = 'REQUEST'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportRequest {

  public enum SupportStatus { OPEN, ACCEPTED, CANCELLED, COMPLETED }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "interaction_id", updatable = false, nullable = false)
  private UUID requestId;

  @Column(name = "user_id", nullable = false)
  private UUID requesterUserId;

  @Transient
  private String supportType;

  @Transient
  private String description;

  @Column(name = "message", columnDefinition = "text")
  private String canonicalMessage;

  @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
  private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
  private BigDecimal longitude;

  @Transient
  @Builder.Default
  private String consentStatus = "PENDING";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private SupportStatus status = SupportStatus.OPEN;

  @Transient
  private LocalDateTime respondedAt;

  @Transient
  private LocalDateTime completedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Builder.Default
  @Column(name = "interaction_type", nullable = false, updatable = false, length = 30)
  private String interactionType = "REQUEST";

  @PrePersist
  @PreUpdate
  void prepareCanonicalMessage() {
    canonicalMessage = String.join("\n",
        "CB1",
        encode(supportType),
        encode(description),
        encode(consentStatus),
        encode(respondedAt == null ? null : respondedAt.toString()),
        encode(completedAt == null ? null : completedAt.toString()));
  }

  @PostLoad
  void hydrateCanonicalMessage() {
    if (canonicalMessage == null) return;
    if (canonicalMessage.startsWith("CB1\n")) {
      String[] parts = canonicalMessage.split("\\n", -1);
      supportType = decode(parts, 1);
      description = decode(parts, 2);
      String consent = decode(parts, 3);
      consentStatus = consent == null || consent.isBlank() ? "PENDING" : consent;
      respondedAt = dateTime(decode(parts, 4));
      completedAt = dateTime(decode(parts, 5));
      return;
    }
    int separator = canonicalMessage.indexOf('\n');
    if (separator < 0) {
      applyLegacyDefaults();
      description = canonicalMessage;
      return;
    }
    try {
      supportType = new String(java.util.Base64.getDecoder().decode(
          canonicalMessage.substring(0, separator)), java.nio.charset.StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ignored) {
      supportType = "LEGACY_UNSPECIFIED";
    }
    consentStatus = "PENDING";
    description = canonicalMessage.substring(separator + 1);
    inferLegacyLifecycleTimes();
  }

  private void applyLegacyDefaults() {
    supportType = "LEGACY_UNSPECIFIED";
    consentStatus = "PENDING";
    inferLegacyLifecycleTimes();
  }

  private void inferLegacyLifecycleTimes() {
    respondedAt = status == SupportStatus.ACCEPTED || status == SupportStatus.COMPLETED
        ? updatedAt : null;
    completedAt = status == SupportStatus.COMPLETED ? updatedAt : null;
  }

  private String encode(String value) {
    return java.util.Base64.getEncoder().encodeToString(
        (value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private String decode(String[] parts, int index) {
    if (index >= parts.length || parts[index].isBlank()) return null;
    return new String(java.util.Base64.getDecoder().decode(parts[index]),
        java.nio.charset.StandardCharsets.UTF_8);
  }

  private LocalDateTime dateTime(String value) {
    return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
  }
}
