package com.carebridge.backend.content.entity;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@SQLRestriction("entry_type = 'CHECKLIST_ENTRY'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_template_id")
    private ChecklistTemplate template;

    @Column(name = "title", length = 500)
    private String itemText;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_function_code", length = 40)
    private ChecklistSupportFunction supportFunction;

    @Column(name = "display_order")
    private Integer order;

    @Column(name = "is_required")
    private Boolean isRequired;

    /**
     * Leaf-level copy of the owning template contract.  The database constraint
     * is intentionally leaf-local, so the ORM must persist the same contract
     * that the root uses (V1 target-bearing, V2 targetless).
     */
    @Builder.Default
    @Column(name = "checklist_contract_version")
    private Short checklistContractVersion = 1;

    @Enumerated(EnumType.STRING)
    /**
     * V1 checklist entries carry a MOTHER/BABY target. Contract V2 entries are
     * targetless recommendation leaves; the owning template's
     * checklistContractVersion is the discriminator. Keep the compatibility
     * builder default for old V1 callers that omit the field.
     */
    @Builder.Default
    @Column(name = "target_subject", nullable = true, length = 10)
    private ChecklistTargetSubject targetSubject = ChecklistTargetSubject.MOTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "due_anchor_type", length = 30)
    private ChecklistAnchorType dueAnchorType;

    @Column(name = "due_offset_start")
    private Integer dueOffsetStart;

    @Column(name = "due_offset_end")
    private Integer dueOffsetEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "due_offset_unit", length = 10)
    private ChecklistRangeUnit dueOffsetUnit;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Builder.Default
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType = "CHECKLIST_ENTRY";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Small authoring-only recurrence projection.  The canonical runtime cadence
     * remains root-owned; this JSON keeps the two item checkboxes without adding
     * another table or confusing due-offset timing with recurrence.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_jsonb", nullable = false, columnDefinition = "jsonb")
    private String configurationJson = "{}";

    /** Compatibility constructor for the pre-detail entity shape. */
    public ChecklistItem(
            UUID id,
            ChecklistTemplate template,
            String itemText,
            Integer order,
            Boolean isRequired,
            ChecklistTargetSubject targetSubject,
            ChecklistAnchorType dueAnchorType,
            Integer dueOffsetStart,
            Integer dueOffsetEnd,
            ChecklistRangeUnit dueOffsetUnit,
            Boolean isActive,
            String entryType,
            Instant createdAt,
            Instant updatedAt) {
         this(id, template, itemText, null, null, order, isRequired, (short) 1, targetSubject,
                 dueAnchorType, dueOffsetStart, dueOffsetEnd, dueOffsetUnit,
                 isActive, entryType, createdAt, updatedAt, "{}");
     }
}
