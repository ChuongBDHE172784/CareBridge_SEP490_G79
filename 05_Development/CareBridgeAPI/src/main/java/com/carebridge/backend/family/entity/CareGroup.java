package com.carebridge.backend.family.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_group_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "group_name", nullable = false, length = 200)
    private String groupName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "linked_journey_id")
    private UUID linkedJourneyId;

    @Column(name = "linked_baby_profile_id")
    private UUID linkedBabyProfileId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CareGroupStatus status = CareGroupStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
