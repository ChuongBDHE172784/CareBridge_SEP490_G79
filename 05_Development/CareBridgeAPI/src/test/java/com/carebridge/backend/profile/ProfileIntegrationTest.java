package com.carebridge.backend.profile;

import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
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

        // DB assertion: PROFILE_UPDATED audit row recorded for this user. Queried with raw SQL
        // scoped to this actor: the shared audit_events table also carries canonical categories
        // written by other suites (e.g. BASELINE_CONTEXT) that are not AuditAction constants,
        // so hydrating every row via auditLogRepository.findAll() would fail on enum mapping.
        entityManager.flush(); // audit row is pending JPA state inside this test transaction
        Integer profileUpdatedAudits = jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where event_origin = 'AUDIT_LOG'
                   and actor_user_id = ?
                   and event_category = 'PROFILE_UPDATED'
                   and resource_type = 'UserProfile'
                """, Integer.class, user.getId());
        assertThat(profileUpdatedAudits).isEqualTo(1);
    }
}
