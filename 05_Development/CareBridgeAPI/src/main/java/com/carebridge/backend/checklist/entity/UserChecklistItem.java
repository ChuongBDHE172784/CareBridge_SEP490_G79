package com.carebridge.backend.checklist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "preparation_checklist_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checklist_item_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "mother_journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "template_entry_id")
    private UUID templateItemId;

    @Column(name = "title", nullable = false, length = 500)
    private String itemText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ChecklistCategory category = ChecklistCategory.GENERAL;

    @Builder.Default
    @Convert(converter = ChecklistCompletionStatusConverter.class)
    @Column(name = "status", nullable = false, length = 30)
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int itemOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
