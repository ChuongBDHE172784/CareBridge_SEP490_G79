package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_record_attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_record_attachment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "health_record_id", nullable = false)
    private UUID healthRecordId;

    @Column(name = "attachment_id", nullable = false)
    private UUID fileId;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
