package com.carebridge.backend.security.entity;

import com.carebridge.backend.security.rbac.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "auth_challenges")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    public enum OtpPurpose {
        REGISTER,
        LOGIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "challenge_id")
    private UUID id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "challenge_hash", nullable = false, length = 255)
    private String codeHash;

    @jakarta.persistence.Transient
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 40)
    private OtpPurpose purpose;

    @jakarta.persistence.Transient
    private String email;

    @Column(name = "subject_identifier", length = 255)
    private String subjectIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", length = 40)
    private Role requestedRole;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @jakarta.persistence.Transient
    private boolean verified = false;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    void canonicalState() {
        if (subjectIdentifier == null) subjectIdentifier = email != null ? email : phone;
        status = usedAt != null ? "USED" : verified ? "VERIFIED" :
                expiresAt != null && !expiresAt.isAfter(Instant.now()) ? "EXPIRED" : "PENDING";
    }

    @jakarta.persistence.PostLoad
    void compatibilityState() {
        verified = "VERIFIED".equals(status) || "USED".equals(status);
        if (subjectIdentifier != null && subjectIdentifier.contains("@")) email = subjectIdentifier;
        else phone = subjectIdentifier;
    }
}
