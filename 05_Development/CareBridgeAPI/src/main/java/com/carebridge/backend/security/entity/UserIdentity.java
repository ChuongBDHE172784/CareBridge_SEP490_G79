package com.carebridge.backend.security.entity;

import com.carebridge.backend.security.federation.FederatedProvider;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_identities_provider_subject", columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(name = "uk_user_identities_user_provider", columnNames = {"user_id", "provider"})})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserIdentity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "identity_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FederatedProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_phone", length = 30)
    private String providerPhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;
}
