package com.carebridge.backend.security.integration;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PROF-TC-008-INT-001 — Integration: an authenticated GET /profile resolves the JWT
 * against a real PostgreSQL-persisted user (Testcontainers), returns the caller's
 * profile, does not leak the password hash, and records a PROFILE_VIEWED audit row
 * per ADR-008-002.
 *
 * <p>Two real bugs were found and fixed via this test: (1) {@code AuditEligibilityPolicy}
 * did not include {@code PROFILE_VIEWED}/{@code PROFILE_UPDATED} in its allowlist, so
 * {@code AuditService.log()} was silently no-op'ing for both regardless of transaction
 * state; (2) {@code AuthServiceImpl.getProfile} ran in a {@code @Transactional(readOnly =
 * true)} transaction, which sets Hibernate's flush mode to MANUAL, so even after fixing
 * (1) the audit insert was enqueued but never flushed/committed. Fixed by removing
 * {@code readOnly} from that method (it now has a legitimate write side effect).
 */
@Transactional
class AuthProfileIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.profile@test.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void getProfile_returnsProfileForAuthenticatedUser_withoutLeakingPasswordHash() throws Exception {
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

        MvcResult result = mockMvc.perform(get("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.role").value("MOTHER"))
                .andReturn();

        // Response must not leak the password hash
        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash");

        // DB assertion: PROFILE_VIEWED audit row recorded (ADR-008-002)
        List<AuditLog> auditLogs = auditLogRepository.findAll().stream()
                .filter(a -> user.getId().equals(a.getActorUserId()))
                .filter(a -> a.getAction() == AuditAction.PROFILE_VIEWED)
                .toList();
        assertThat(auditLogs).hasSize(1);
    }
}
