package com.carebridge.backend.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID profileId;

    @jakarta.persistence.Transient
    private UUID userId;

    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "phone", length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 100)
    @Column(name = "area", length = 100)
    private String area;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void prepareCanonicalUser() {
        if (profileId == null) profileId = userId;
        if (userId == null) userId = profileId;
    }

    @PostLoad
    void hydrateCanonicalUser() {
        userId = profileId;
    }
}
