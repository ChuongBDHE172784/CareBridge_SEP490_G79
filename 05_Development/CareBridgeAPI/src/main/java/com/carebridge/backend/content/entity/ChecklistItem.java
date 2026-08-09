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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;

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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "target_subject", nullable = false, length = 10)
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
        this(id, template, itemText, null, null, order, isRequired, targetSubject,
                dueAnchorType, dueOffsetStart, dueOffsetEnd, dueOffsetUnit,
                isActive, entryType, createdAt, updatedAt);
    }
}
