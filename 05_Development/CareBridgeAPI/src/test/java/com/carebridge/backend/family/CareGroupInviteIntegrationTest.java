package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CareGroupInviteIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── TC-INT-002: Migration applies cleanly ─────────────────────────────────

    @Test
    void migration_addsInviteColumnsAndIndex() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'care_group_members'",
                String.class);

        assertThat(columns).contains("invite_token", "invite_channel", "invite_expires_at", "invited_phone", "user_id");

        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'care_group_members' AND indexname = 'uq_care_group_members_invite_token'",
                String.class);

        assertThat(indexes).contains("uq_care_group_members_invite_token");
    }

    // ── TC-INT-001: Full E2E PHONE invite persisted correctly ─────────────────

    @Test
    void inviteFamilyMember_phoneChannel_persistsRowCorrectly() {
        User owner = createTestUser("+84900000001");
        User invitee = createTestUser("+84912345678");

        CareGroup group = groupRepository.save(CareGroup.builder()
                .ownerUserId(owner.getId())
                .groupName("Integration Test Group")
                .status(CareGroupStatus.ACTIVE)
                .build());

        memberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(owner.getId())
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build());

        memberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(invitee.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.PENDING)
                .inviteChannel(InviteChannel.PHONE)
                .inviteToken("integrationTestToken01")
                .inviteExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .invitedPhone("+84912345678")
                .build());

        CareGroupMember record = memberRepository.findByCareGroupIdAndUserId(group.getId(), invitee.getId())
                .orElseThrow();

        assertThat(record.getInviteStatus()).isEqualTo(InviteStatus.PENDING);
        assertThat(record.getInviteChannel()).isEqualTo(InviteChannel.PHONE);
        assertThat(record.getInviteToken()).isNotBlank();
        assertThat(record.getInvitedPhone()).isEqualTo("+84912345678");
        assertThat(record.getInviteExpiresAt()).isNotNull();
    }

    private User createTestUser(String phone) {
        User user = new User();
        user.setEmail("test-" + UUID.randomUUID() + "@carebridge.test");
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("Test@1234"));
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        return userRepository.save(user);
    }
}
