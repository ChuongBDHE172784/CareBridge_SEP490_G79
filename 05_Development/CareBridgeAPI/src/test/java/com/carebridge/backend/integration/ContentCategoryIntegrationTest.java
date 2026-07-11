package com.carebridge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ContentCategoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CommunityTopicRepository topicRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    @Test
    void createCategory_persistsThroughContentAdminRoute() throws Exception {
        String token = seedContentAdmin("category.persist@test.com");
        mockMvc.perform(post("/api/v1/admin/content/categories").with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Postpartum Recovery","description":"Verified recovery guidance","sortOrder":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Postpartum Recovery"));

        assertThat(topicRepository.existsByNameIgnoreCase("Postpartum Recovery")).isTrue();
    }

    @Test
    void createCategory_isImmediatelyVisibleThroughCommunityTopicRoute() throws Exception {
        String token = seedContentAdmin("category.crossroute@test.com");
        mockMvc.perform(post("/api/v1/admin/content/categories").with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Infant Nutrition","description":"Verified nutrition guidance","sortOrder":8}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/community/topics")
                        .header("Authorization", "Bearer " + token)
                        .param("includeHidden", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Infant Nutrition')]").exists());
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
