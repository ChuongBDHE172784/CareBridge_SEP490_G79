package com.carebridge.backend.content.entity;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "care_item_templates")
@SQLRestriction("entry_type = 'TEMPLATE_ROOT'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", length = 500)
    private String name;

    @Column(name = "template_lineage_id")
    private UUID templateLineageId;

    @Column(name = "template_version_id")
    private UUID templateVersionId;

    @Column(name = "substage_id")
    private UUID substageId;

    /** Positive position in the PRE_PREGNANCY sequence; zero is legacy/unsequenced. */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer sequencePosition = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_scope", length = 10)
    private ChecklistRecipientScope recipientScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_anchor_type", length = 30)
    private ChecklistAnchorType eligibilityAnchorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_range_unit", length = 10)
    private ChecklistRangeUnit eligibilityRangeUnit;

    @Column(name = "eligibility_start_inclusive")
    private Integer eligibilityStartInclusive;

    @Column(name = "eligibility_end_inclusive")
    private Integer eligibilityEndInclusive;

    /** Root-owned cadence declaration. Checklist items never map these columns. */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    private ChecklistScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "materialization_policy", length = 30)
    private ChecklistMaterializationPolicy materializationPolicy;

    @Column(name = "schedule_group_key", length = 120)
    private String scheduleGroupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_context_type", length = 10)
    private ChecklistCareContextType scheduleContextType;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_end_mode", length = 20)
    private ChecklistScheduleEndMode scheduleEndMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_boundary_rule", length = 30)
    private ChecklistWeekBoundaryRule weekBoundaryRule;

    @Column(name = "checklist_contract_version")
    private Short checklistContractVersion;

    /** Canonical root copy/provenance metadata (CHECKLIST_METADATA_V1). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_metadata_jsonb", columnDefinition = "jsonb")
    private String checklistMetadataJson;

    @Column(name = "checklist_metadata_hash", length = 128)
    private String checklistMetadataHash;

    @Column(name = "checklist_quarantine_reason_code", length = 80)
    private String checklistQuarantineReasonCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private ContentStage stage;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "content_status", nullable = false, length = 20)
    private ChecklistTemplateStatus status = ChecklistTemplateStatus.DRAFT;

    @Builder.Default
    @Column(name = "migration_review_required", nullable = false)
    private Boolean migrationReviewRequired = Boolean.FALSE;

    @Builder.Default
    @Column(name = "distribution_enabled", nullable = false)
    private Boolean distributionEnabled = Boolean.FALSE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 20)
    private ChecklistTemplateType templateType = ChecklistTemplateType.MANDATORY;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "assigned_expert_id")
    private UUID assignedExpertId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "migration_reviewed_at")
    private Instant migrationReviewedAt;

    @Column(name = "migration_reviewed_by")
    private UUID migrationReviewedBy;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer versionNo = 1;

    @Builder.Default
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType = "TEMPLATE_ROOT";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "revision_reason", columnDefinition = "TEXT")
    private String revisionReason;

    @Column(name = "revision_requested_at")
    private Instant revisionRequestedAt;

    @Column(name = "revision_requested_by")
    private UUID revisionRequestedBy;

    @Column(name = "revision_requested_version")
    private Integer revisionRequestedVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
