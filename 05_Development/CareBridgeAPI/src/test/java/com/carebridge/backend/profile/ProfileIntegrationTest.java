package com.carebridge.backend.profile;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRF-TC-INT-001 — Integration: PATCH /api/v1/profile persists to the real database
 * (Testcontainers PostgreSQL) and records a PROFILE_UPDATED audit entry.
 *
 * <p>Unlike UC-08's GET /profile (read-only transaction, audit never flushed), UC-09's
 * {@code ProfileServiceImpl.updateProfile} runs in a plain {@code @Transactional} write
 * transaction, so both the canonical person row and the {@code audit_logs} row are
 * actually committed and observable here.
 */
@Transactional
class ProfileIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.profile.update@test.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void patchProfile_fullStack_persistsToDbAndRecordsAudit() throws Exception {
        User user = userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(patch("/api/v1/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Nguyen Test\",\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Nguyen Test"))
                .andExpect(jsonPath("$.data.phoneNumber").value("0912345678"));

        // Account name and profile-specific fields are persisted separately.
        UserProfile saved = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(saved.getPhoneNumber()).isEqualTo("0912345678");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getName())
                .isEqualTo("Nguyen Test");

        // DB assertion: PROFILE_UPDATED audit row recorded for this user
        // (query directly rather than via the repository's `search` method: its optional
        // null-date filters use "? is null or ..." params, which the real Postgres driver
        // cannot type-infer without an explicit cast — fine on H2, fails on Postgres)
        List<AuditLog> auditLogs = auditLogRepository.findAll().stream()
                .filter(a -> user.getId().equals(a.getActorUserId()))
                .filter(a -> a.getAction() == AuditAction.PROFILE_UPDATED)
                .toList();
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getEntityType()).isEqualTo("UserProfile");
    }
}
