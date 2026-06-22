package com.carebridge.backend.partner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partner_expert_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerExpertLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "partner_expert_link_id")
    private UUID partnerExpertLinkId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "relationship_type", length = 40)
    private String relationshipType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
