package com.carebridge.backend.partner.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
@Entity @Table(name="sponsored_campaigns") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SponsoredCampaign {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="campaign_id") private UUID id;
 @Column(name="partner_id",nullable=false) private UUID partnerId;
 @Column(nullable=false,length=255) private String title;
 @Column(columnDefinition="text") private String description;
 @Column(name="start_date") private LocalDate startDate;
 @Column(name="end_date") private LocalDate endDate;
 @Column(name="sponsor_label",length=100) private String sponsorLabel;
 @Enumerated(EnumType.STRING) @Column(name="approval_status",nullable=false,length=30) private CampaignApprovalStatus approvalStatus;
 @Column(name="reviewed_by") private UUID reviewedBy;
 @Builder.Default @Column(name="is_removed",nullable=false) private boolean removed=false;
 @Column(name="removed_at") private Instant removedAt;
 @Column(name="removed_by") private UUID removedBy;
 @Column(name="removal_reason",columnDefinition="text") private String removalReason;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @UpdateTimestamp @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
