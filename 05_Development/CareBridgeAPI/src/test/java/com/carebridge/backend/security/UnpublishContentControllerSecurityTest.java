package com.carebridge.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentUnpublishController;
import com.carebridge.backend.content.service.ContentUnpublishService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(value = ContentUnpublishController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UnpublishContentControllerSecurityTest {
    static final String URL = "/api/v1/admin/content/f1b00000-0000-0000-0000-000000000001/unpublish";
    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean ContentUnpublishService service;
    @MockitoBean JwtTokenProvider jwt;
    @MockitoBean UserRepository users;

    @Test @WithMockUser(roles = "MODERATOR") void upcTc1208_moderatorForbidden() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"outdated\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void upcTc1209_noJwtUnauthorized() throws Exception {
        assertTrue(mappings.getHandlerMethods().keySet().stream().anyMatch(info ->
                info.getPatternValues().contains("/api/v1/admin/content/{id}/unpublish")
                        && info.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"outdated\"}"))
                .andExpect(status().isUnauthorized());
    }
}
