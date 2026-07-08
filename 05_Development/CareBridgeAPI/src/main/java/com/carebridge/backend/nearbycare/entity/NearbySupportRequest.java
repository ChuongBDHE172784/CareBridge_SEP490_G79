package com.carebridge.backend.nearbycare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nearby_support_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportRequest {

  public enum SupportStatus { OPEN, ACCEPTED, CANCELLED, COMPLETED }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "request_id", updatable = false, nullable = false)
  private UUID requestId;

  @Column(name = "requester_user_id", nullable = false)
  private UUID requesterUserId;

  @Column(name = "support_type", nullable = false, length = 50)
  private String supportType;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
  private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
  private BigDecimal longitude;

  @Column(name = "consent_status", nullable = false, length = 20)
  @Builder.Default
  private String consentStatus = "PENDING";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private SupportStatus status = SupportStatus.OPEN;

  @Column(name = "responded_at")
  private LocalDateTime respondedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
