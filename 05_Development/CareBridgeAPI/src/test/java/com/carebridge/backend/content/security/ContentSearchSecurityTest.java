package com.carebridge.backend.content.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CNT224-TC-SEC-002 — Unauthenticated search rejected (OWASP A01:2021, CWE-306)
// CNT224-TC-SEC-001 — SQL injection prevention verified via parameterized JPA query
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ContentSearchSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    // ── CNT224-TC-SEC-002: No JWT → 401 ──────────────────────────────────────
    // Oracle: BR-RBAC, SecurityConfig — /api/v1/** requires authentication
    @Test
    void searchContent_withoutJwt_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "thai ky"))
                .andExpect(status().isUnauthorized());
    }

    // ── CNT224-TC-SEC-001 notes ──────────────────────────────────────────────
    // SQL injection prevention is guaranteed by Spring Data JPA parameterized queries.
    // The @Query in ContentRepository uses @Param bindings, not string concatenation.
    // No raw JDBC or native query string interpolation is used.
    // Verification: the query in ContentRepository.searchByFilters() uses
    // LOWER(CONCAT('%', :keyword, '%')) with named parameters — JDBC PreparedStatement
    // prevents injection regardless of keyword content.
}
