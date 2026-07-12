package com.carebridge.backend.partner.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity @Table(name = "partner_services")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartnerService {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "service_id") private UUID id;
    @Column(name = "partner_id", nullable = false) private UUID partnerId;
    @Column(name = "service_name", nullable = false, length = 200) private String serviceName;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "price_from") private BigDecimal priceFrom;
    @Column(length = 10) private String currency;
    @Column(name = "booking_url", columnDefinition = "text") private String bookingUrl;
    @Enumerated(EnumType.STRING) @Column(name = "approval_status", nullable = false, length = 30) private ServiceApprovalStatus approvalStatus;
    @Builder.Default @Column(name = "is_removed", nullable = false) private boolean removed = false;
    @Column(name = "removed_at") private Instant removedAt;
    @Column(name = "removed_by") private UUID removedBy;
    @Column(name = "removal_reason", columnDefinition = "text") private String removalReason;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
