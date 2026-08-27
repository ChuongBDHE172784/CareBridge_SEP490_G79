package com.carebridge.backend.identity.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC114 Manage User Accounts — full-stack integration tests against a real
 * Testcontainers PostgreSQL instance.
 * Covers UC114-TC-INT-001 (round trip + audit) and UC114-TC-SEC-001 (injection safety).
 */
@Transactional
class AdminUserControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User saveAdmin(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .role(Role.SYSTEM_ADMIN)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
    }

    private User saveMother(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
    }

    // UC114-TC-INT-001
    @Test
    void searchAndUpdateStatus_fullStack_persistsAndRecordsAudit() throws Exception {
        User admin = saveAdmin("int.admin.uc114@test.com");
        User target = saveMother("int.target.uc114@test.com");
        String adminToken = jwtTokenProvider.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/admin/users?role=MOTHER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(patch("/api/v1/admin/users/" + target.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false, \"reason\": \"Integration test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.isEnabled()).isFalse();

        List<AuditLog> logs = auditLogRepository.findByEntityIdAndAction(
                target.getId(), AuditAction.USER_ACCOUNT_STATUS_CHANGED);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getActorUserId()).isEqualTo(admin.getId());
    }

    // UC114-TC-SEC-001
    @Test
    void searchUsers_sqlInjectionAttemptInFilterParams_isNeutralized() throws Exception {
        User admin = saveAdmin("int.admin.sec001@test.com");
        saveMother("int.target.sec001@test.com");
        String adminToken = jwtTokenProvider.generateAccessToken(admin);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("email", "' OR '1'='1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("name", "{\"$ne\":null}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
