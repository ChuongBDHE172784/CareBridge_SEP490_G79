package com.carebridge.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checklist_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checklist_item_id")
    private UUID checklistItemId;

    @Column(name = "checklist_template_id")
    private UUID checklistTemplateId;

    @Column(name = "item_order")
    private Integer itemOrder;

    @Column(name = "item_text", length = 500)
    private String itemText;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
