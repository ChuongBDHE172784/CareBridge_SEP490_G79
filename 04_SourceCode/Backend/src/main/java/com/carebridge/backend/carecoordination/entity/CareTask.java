package com.carebridge.backend.carecoordination.entity;

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
@Table(name = "care_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_task_id")
    private UUID careTaskId;

    @Column(name = "care_group_id")
    private UUID careGroupId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
