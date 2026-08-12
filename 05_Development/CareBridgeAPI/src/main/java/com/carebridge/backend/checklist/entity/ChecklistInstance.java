package com.carebridge.backend.checklist.entity;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "checklist_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checklist_instance_id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "distribution_key", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String distributionKey;

    @Builder.Default
    @Column(name = "key_version", nullable = false, length = 10)
    private String keyVersion = "v1";

    @Column(name = "template_lineage_id")
    private UUID templateLineageId;

    @Column(name = "template_version_id")
    private UUID templateVersionId;

    /** Canonical cadence period identity; null for retained legacy aggregates. */
    @Column(name = "period_key", length = 180)
    private String periodKey;

    /** Schedule-zone snapshot used to resolve this occurrence. */
    @Column(name = "schedule_zone_id", length = 80)
    private String scheduleZoneId;

    /** Pregnancy dating revision used by this occurrence, when applicable. */
    @Column(name = "gestational_dating_revision")
    private Long gestationalDatingRevision;

    /** Soft-retained Family membership owner for this parent, when applicable. */
    @Column(name = "care_group_member_id")
    private UUID careGroupMemberId;

    /** VIEW epoch captured when a Family parent was materialized. */
    @Column(name = "checklist_access_epoch")
    private Long checklistAccessEpoch;

    @Column(name = "checklist_contract_version")
    private Short checklistContractVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "materialization_mode", length = 20)
    private ChecklistMaterializationMode materializationMode;

    /** False for closed catch-up rows that never had an action window. */
    @Column(name = "was_actionable")
    private Boolean wasActionable;

    @Column(name = "checklist_quarantine_reason_code", length = 80)
    private String checklistQuarantineReasonCode;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 10)
    private ChecklistRecipientRole recipientRole;

    @Column(name = "care_group_id")
    private UUID careGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_context_type", nullable = false, length = 10)
    private ChecklistCareContextType careContextType;

    @Column(name = "care_context_id", nullable = false)
    private UUID careContextId;

    @Column(name = "context_owner_user_id", nullable = false)
    private UUID contextOwnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private ChecklistOrigin origin;

    @Column(name = "window_start")
    private LocalDate windowStart;

    @Column(name = "window_end")
    private LocalDate windowEnd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChecklistInstanceStatus status = ChecklistInstanceStatus.PENDING;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason_code", length = 80)
    private String cancellationReasonCode;

    @Column(name = "historical_at")
    private Instant historicalAt;

    @Column(name = "history_reason_code", length = 80)
    private String historyReasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
