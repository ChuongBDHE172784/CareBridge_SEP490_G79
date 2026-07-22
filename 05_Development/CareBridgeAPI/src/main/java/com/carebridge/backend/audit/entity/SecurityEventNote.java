package com.carebridge.backend.audit.entity;

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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", updatable = false, nullable = false)
    private UUID noteId;

    @Column(name = "security_event_id", nullable = false)
    private Long eventId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID authorId;

    @Column(name = "note_text", nullable = false, columnDefinition = "text")
    private String noteText;

    @Builder.Default
    @Column(name = "event_category", nullable = false, length = 80)
    private String eventCategory = "SECURITY_INVESTIGATION_NOTE";

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant createdAt;
}
