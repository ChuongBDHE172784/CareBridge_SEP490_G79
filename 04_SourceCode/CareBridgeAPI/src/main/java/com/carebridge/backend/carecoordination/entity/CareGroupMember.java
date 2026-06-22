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
@Table(name = "care_group_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_group_member_id")
    private UUID careGroupMemberId;

    @Column(name = "care_group_id")
    private UUID careGroupId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "member_role", length = 30)
    private String memberRole;

    @Column(name = "invitation_status", length = 20)
    private String invitationStatus;

    @Column(name = "permission_json", columnDefinition = "jsonb")
    private String permissionJson;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
