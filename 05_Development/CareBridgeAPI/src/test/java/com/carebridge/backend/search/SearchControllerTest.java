package com.carebridge.backend.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.search.controller.SearchController;
import com.carebridge.backend.search.dto.response.PaginationMeta;
import com.carebridge.backend.search.dto.response.SearchResultResponse;
import com.carebridge.backend.search.entity.SearchType;
import com.carebridge.backend.search.service.SearchService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SEARCH-TC-013-002..005 (Test-Spec §4). Controller-level validation and auth checks —
 * no query logic in the controller itself (BR — controller delegates only).
 */
@WebMvcTest(
        value = SearchController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SearchControllerTest {

    private static final String BASE_URL = "/api/v1/search";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    // SEARCH-TC-013-002: blank q → SEARCH-001
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void search_blankQuery_returns400SearchOne() throws Exception {
        mockMvc.perform(get(BASE_URL).param("q", "").param("type", "QUESTION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SEARCH-001"));

        verify(searchService, never()).search(any(), any());
    }

    // SEARCH-TC-013-003: invalid type → SEARCH-002
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void search_invalidType_returns400SearchTwo() throws Exception {
        mockMvc.perform(get(BASE_URL).param("q", "test").param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SEARCH-002"));

        verify(searchService, never()).search(any(), any());
    }

    // SEARCH-TC-013-004: negative page → SEARCH-003
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void search_negativePage_returns400SearchThree() throws Exception {
        mockMvc.perform(get(BASE_URL).param("q", "test").param("type", "QUESTION").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SEARCH-003"));

        verify(searchService, never()).search(any(), any());
    }

    // SEARCH-TC-013-005: no JWT → 401
    @Test
    void search_noJwt_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL).param("q", "test").param("type", "QUESTION"))
                .andExpect(status().isUnauthorized());

        verify(searchService, never()).search(any(), any());
    }

    // Happy path: valid request delegates to service and returns 200
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void search_validRequest_returns200() throws Exception {
        SearchResultResponse response = SearchResultResponse.builder()
                .type(SearchType.QUESTION)
                .items(Collections.emptyList())
                .pagination(new PaginationMeta(0, 20, 0, 0))
                .build();
        when(searchService.search(any(), any())).thenReturn(response);

        mockMvc.perform(get(BASE_URL).param("q", "thai kỳ").param("type", "QUESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("QUESTION"));
    }
}
