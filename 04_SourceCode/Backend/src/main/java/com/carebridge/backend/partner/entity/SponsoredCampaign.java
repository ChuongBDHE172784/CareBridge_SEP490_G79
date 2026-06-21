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
@Table(name = "sponsored_campaigns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsoredCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "title", length = 250)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "sponsor_label", length = 150)
    private String sponsorLabel;

    @Column(name = "approval_status", length = 20)
    private String approvalStatus;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
