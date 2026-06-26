package com.carebridge.backend.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.service.CommunityFeedService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// COM198-TC-005, COM198-TC-SEC-001
@WebMvcTest(
        value = CommunityFeedController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class CommunityFeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityFeedService feedService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String FEED_URL = "/api/v1/community/feed";

    private PaginatedResponse<CommunityFeedItemResponse> emptyPagedResponse() {
        return PaginatedResponse.of(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));
    }

    // COM198-TC-SEC-001: No JWT → 401
    @Test
    void getFeed_noJwt_returns401() throws Exception {
        mockMvc.perform(get(FEED_URL))
                .andExpect(status().isUnauthorized());
    }

    // COM198-TC-005a: size > 50 → 400 COM-001
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getFeed_sizeOver50_returns400WithCom001() throws Exception {
        mockMvc.perform(get(FEED_URL).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COM-001"));
    }

    // COM198-TC-005b: size = 50 → 200 (boundary — allowed)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getFeed_sizeExactly50_returns200() throws Exception {
        when(feedService.getFeed(any(), eq(0), eq(50))).thenReturn(emptyPagedResponse());

        mockMvc.perform(get(FEED_URL).param("size", "50"))
                .andExpect(status().isOk());
    }

    // Happy path: authenticated → 200
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getFeed_authenticated_returns200() throws Exception {
        when(feedService.getFeed(any(), eq(0), eq(20))).thenReturn(emptyPagedResponse());

        mockMvc.perform(get(FEED_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // EXPERT role → 200 (all authenticated roles allowed)
    @Test
    @WithMockUser(username = "2", roles = "EXPERT")
    void getFeed_expertRole_returns200() throws Exception {
        when(feedService.getFeed(any(), anyInt(), anyInt())).thenReturn(emptyPagedResponse());

        mockMvc.perform(get(FEED_URL))
                .andExpect(status().isOk());
    }

    // size = 0 → 400 COM-001
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getFeed_sizeZero_returns400WithCom001() throws Exception {
        mockMvc.perform(get(FEED_URL).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COM-001"));
    }
}
