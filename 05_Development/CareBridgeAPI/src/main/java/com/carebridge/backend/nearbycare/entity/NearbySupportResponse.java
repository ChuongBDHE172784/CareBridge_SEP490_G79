package com.carebridge.backend.nearbycare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nearby_support_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportResponse {

    public enum ResponseAction { ACCEPT, DECLINE, STOP }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "response_id", updatable = false, nullable = false)
    private UUID responseId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ResponseAction action;

    @Column(columnDefinition = "text")
    private String note;

    @CreationTimestamp
    @Column(name = "responded_at", nullable = false, updatable = false)
    private LocalDateTime respondedAt;
}
