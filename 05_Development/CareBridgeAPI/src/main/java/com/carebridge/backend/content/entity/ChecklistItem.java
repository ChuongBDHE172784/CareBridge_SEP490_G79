package com.carebridge.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    @Column(name = "display_order")
    private Integer order;

    @Column(name = "is_required")
    private Boolean isRequired;

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
}
