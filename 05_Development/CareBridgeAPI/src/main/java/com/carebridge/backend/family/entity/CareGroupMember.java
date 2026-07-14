package com.carebridge.backend.family.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


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
    @Column(name = "care_group_member_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_group_id", nullable = false)
    private UUID careGroupId;

    /** Maps to V1 column user_id (represents the member's account). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", length = 50)
    private GroupMemberRole memberRole;

    /** Maps to V1 column invitation_status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 20)
    private InviteStatus inviteStatus;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "invite_token", length = 64)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_channel", length = 20)
    private InviteChannel inviteChannel;

    @Column(name = "invite_expires_at")
    private Instant inviteExpiresAt;

    @Column(name = "invited_phone", length = 20)
    private String invitedPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permission_json", columnDefinition = "jsonb")
    private String permissionJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
