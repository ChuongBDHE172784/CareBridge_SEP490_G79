package com.carebridge.backend.content;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentUnpublishController;
import com.carebridge.backend.content.dto.response.UnpublishResponse;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.service.ContentUnpublishService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ContentUnpublishController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UnpublishContentControllerTest {
    static final UUID ADMIN = UUID.fromString("f1a00000-0000-0000-0000-0000000000ad");
    static final UUID CONTENT = UUID.fromString("f1b00000-0000-0000-0000-000000000001");
    @Autowired MockMvc mvc;
    @MockitoBean ContentUnpublishService service;
    @MockitoBean JwtTokenProvider jwt;
    @MockitoBean UserRepository users;

    @Test @WithMockUser(username = "f1a00000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void upcTc1201_returnsArchivedResponse() throws Exception {
        Instant published = Instant.parse("2026-05-01T08:00:00Z");
        when(service.unpublish(eq(CONTENT), any(), eq(ADMIN))).thenReturn(new UnpublishResponse(CONTENT,
                ContentStatus.APPROVED, ContentStatus.ARCHIVED, published, ADMIN, "outdated", Instant.now()));
        mvc.perform(post(url()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"outdated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.newStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.publishedAt").value("2026-05-01T08:00:00Z"));
    }

    @Test @WithMockUser(roles = "CONTENT_ADMIN")
    void upcTc1205_blankReasonReturns400() throws Exception {
        mvc.perform(post(url()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
    static String url() { return "/api/v1/admin/content/" + CONTENT + "/unpublish"; }
}
