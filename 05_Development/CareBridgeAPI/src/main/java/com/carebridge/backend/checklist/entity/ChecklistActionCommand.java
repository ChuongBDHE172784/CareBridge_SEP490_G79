package com.carebridge.backend.checklist.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "checklist_action_commands")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChecklistActionCommand {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checklist_action_command_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;
    @Column(name = "task_kind", nullable = false, length = 30)
    private String taskKind;
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    @Column(name = "reminder_definition_id")
    private UUID reminderDefinitionId;
    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payload_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String payloadHash;
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;
    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus;
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "result_jsonb", nullable = false, columnDefinition = "jsonb")
    private String resultJson = "{}";
    @Builder.Default @CreationTimestamp @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();
    @Column(name = "retain_until", nullable = false)
    private Instant retainUntil;
    @Builder.Default @Column(name = "legal_hold", nullable = false)
    private Boolean legalHold = Boolean.FALSE;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
