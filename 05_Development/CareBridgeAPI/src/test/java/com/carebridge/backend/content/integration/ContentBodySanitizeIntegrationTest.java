package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// RTE-TC-015 / RTE-TC-016 — ContentRichTextEditor_Test-Spec.md §4, full stack against real
// Testcontainers PostgreSQL. Guards ADR-RTE-005 (server-side HTML sanitize) end-to-end: an
// attacker-controlled CONTENT_ADMIN session must not be able to get a <script> tag into the
// stored body, since it is later rendered unescaped (web dangerouslySetInnerHTML, mobile
// flutter_html).
@Transactional
class ContentBodySanitizeIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentRepository contentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    private static final String BASE_URL = "/api/v1/admin/content";

    // RTE-TC-015
    @Test
    void createContent_maliciousHtmlBody_persistedBodyIsSanitized() throws Exception {
        String token = seedUser("rte.xss@test.com", Role.CONTENT_ADMIN);

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ARTICLE","title":"RTE sanitize integration test",
                                 "body":"<p>Xin chào</p><script>alert(document.cookie)</script>",
                                 "stage":"PREGNANCY"}
                                """))
                .andExpect(status().isCreated());

        List<ContentItem> saved = contentRepository.findAll().stream()
                .filter(c -> "RTE sanitize integration test".equals(c.getTitle()))
                .toList();
        assertThat(saved).hasSize(1);
        ContentItem item = saved.get(0);
        assertThat(item.getBody()).doesNotContain("<script");
        assertThat(item.getBody()).doesNotContain("alert(document.cookie)");
        assertThat(item.getBody()).contains("Xin chào");
    }

    // RTE-TC-016 — needs real Cloudinary credentials to exercise the network upload call; the
    // URL-persistence fix itself (ADR-RTE-004) is already covered without network dependency by
    // CloudinaryStorageServiceTest (RTE-TC-008/009/010, unit-level, GREEN). Per
    // ContentRichTextEditor_Test-Spec.md §4 note: disabled rather than deleted when no credential
    // is wired for Testcontainers-based integration tests in this repo.
    @Test
    @Disabled("Requires real CLOUDINARY_* credentials wired into the Testcontainers test context — "
            + "not configured in this repo's test profile. See ContentRichTextEditor_Test-Spec.md RTE-TC-016 note.")
    void uploadPublicContentImage_endToEnd_persistsPublicAccessModeAndPermanentUrl() {
        // Intentionally left unimplemented — see @Disabled reason above.
    }

    private String seedUser(String email, Role role) {
        User user = userRepository.save(User.builder()
                .email(email)
                .role(role)
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
