package com.carebridge.backend.nearbycare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nearby_support_interactions")
@org.hibernate.annotations.SQLRestriction("interaction_type = 'RESPONSE'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportResponse {

    public enum ResponseAction { ACCEPT, DECLINE, STOP }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id", updatable = false, nullable = false)
    private UUID responseId;

    @Column(name = "parent_interaction_id", nullable = false)
    private UUID requestId;

    @Column(name = "user_id", nullable = false)
    private UUID expertProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ResponseAction action;

    @Column(name = "message", columnDefinition = "text")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime respondedAt;

    @Builder.Default
    @Column(name = "interaction_type", nullable = false, updatable = false, length = 30)
    private String interactionType = "RESPONSE";
}
