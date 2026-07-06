package com.carebridge.backend.community.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * COMM-TC-020-INT-001 / COMM-TC-021-INT-001 — full stack: real controller → real service →
 * real repository → Testcontainers PostgreSQL.
 */
@Transactional
class CommunityProfileIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.communityprofile.uc20@test.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityProfileRepository profileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String seedUserAndGetToken() {
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
        return jwtTokenProvider.generateAccessToken(user);
    }

    // COMM-TC-020-INT-001: POST creates a DB row with correct userId and is_visible=true
    @Test
    void createProfile_fullStack_insertsRowInDb() throws Exception {
        String token = seedUserAndGetToken();
        String requestBody = """
                {
                  "displayName": "TestMother20",
                  "bio": "This is a synthetic test bio",
                  "interestStage": "PREGNANCY",
                  "isVisible": true
                }
                """;

        mockMvc.perform(post("/api/v1/community/profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        CommunityProfile saved = profileRepository.findAll().stream()
                .filter(p -> "TestMother20".equals(p.getDisplayName()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.isVisible()).isTrue();
    }

    // COMM-TC-021-INT-001: PUT changes updated_at and is_visible, row still exists (not deleted)
    @Test
    void updateProfile_fullStack_changesUpdatedAtAndKeepsRow() throws Exception {
        String token = seedUserAndGetToken();
        User user = userRepository.findByEmail(EMAIL).orElseThrow();

        CommunityProfile existing = profileRepository.save(CommunityProfile.builder()
                .userId(user.getId())
                .displayName("OriginalName")
                .bio("Original bio")
                .interestStage("PREGNANCY")
                .visible(true)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build());
        Instant originalUpdatedAt = existing.getUpdatedAt();

        String requestBody = """
                {"displayName": "UpdatedName", "isVisible": false}
                """;

        mockMvc.perform(put("/api/v1/community/profiles/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        CommunityProfile updated = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.isVisible()).isFalse();
        assertThat(profileRepository.count()).isEqualTo(1); // hiding never deletes the row
    }
}
