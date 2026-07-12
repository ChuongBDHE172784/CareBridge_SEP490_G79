package com.carebridge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ContentRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UnpublishContentIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentRepository contentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    @Test
    void unpublish_persistsArchivedStatusAndPreservesPublishedAt() throws Exception {
        String token = seedContentAdmin("unpublish.persist@test.com");
        Instant publishedAt = Instant.parse("2026-07-01T00:00:00Z");
        ContentItem item = contentRepository.saveAndFlush(approvedContent("Postpartum Warning Signs", publishedAt));

        mockMvc.perform(post("/api/v1/admin/content/{id}/unpublish", item.getId()).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Superseded by reviewed guidance\"}"))
                .andExpect(status().isOk());

        ContentItem persisted = contentRepository.findById(item.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ContentStatus.ARCHIVED);
        assertThat(persisted.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void unpublish_removesItemFromPublicDetailImmediately() throws Exception {
        String token = seedContentAdmin("unpublish.visibility@test.com");
        ContentItem item = contentRepository.saveAndFlush(
                approvedContent("Infant Hydration Guidance", Instant.parse("2026-07-02T00:00:00Z")));

        mockMvc.perform(get("/api/v1/content/{id}", item.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/content/{id}/unpublish", item.getId()).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Clinical review expired\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/content/{id}", item.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private ContentItem approvedContent(String title, Instant publishedAt) {
        return ContentItem.builder()
                .type(ContentType.ARTICLE)
                .title(title)
                .body("Synthetic verified content for integration testing.")
                .stage(ContentStage.POSTPARTUM)
                .status(ContentStatus.APPROVED)
                .versionNo(1)
                .publishedAt(publishedAt)
                .build();
    }

    private String seedContentAdmin(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .role(Role.CONTENT_ADMIN)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        return jwtTokenProvider.generateAccessToken(user);
    }
}
