package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InviteTokenUniquenessIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── TC-014: Unique index rejects duplicate invite tokens ──────────────────

    @Test
    void inviteToken_duplicateToken_violatesUniqueConstraint() {
        String duplicateToken = "duplicateToken9999";

        User owner = createUser("+84911000001");
        User invitee1 = createUser("+84911000002");
        User invitee2 = createUser("+84911000003");

        CareGroup group = groupRepository.save(CareGroup.builder()
                .ownerUserId(owner.getId())
                .groupName("Uniqueness Test Group")
                .status(CareGroupStatus.ACTIVE)
                .build());

        memberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(invitee1.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.PENDING)
                .inviteToken(duplicateToken)
                .inviteExpiresAt(Instant.now().plusSeconds(86400))
                .build());

        assertThatThrownBy(() -> memberRepository.saveAndFlush(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(invitee2.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.PENDING)
                .inviteToken(duplicateToken)
                .inviteExpiresAt(Instant.now().plusSeconds(86400))
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String phone) {
        User user = new User();
        user.setEmail("tok-" + UUID.randomUUID() + "@carebridge.test");
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("Test@1234"));
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }
}
