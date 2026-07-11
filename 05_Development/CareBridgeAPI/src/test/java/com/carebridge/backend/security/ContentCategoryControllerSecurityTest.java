package com.carebridge.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.service.CommunityTopicService;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentCategoryController;
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

@WebMvcTest(value = ContentCategoryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ContentCategoryControllerSecurityTest {
    static final String URL = "/api/v1/admin/content/categories";
    static final String BODY = "{\"name\":\"Nutrition\"}";
    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean CommunityTopicService topics;
    @MockitoBean AuditService audit;
    @MockitoBean JwtTokenProvider jwt;
    @MockitoBean UserRepository users;

    @Test @WithMockUser(username = "f1800000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void mccTc1107_contentAdminCanAccess() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "MOTHER") void mccTc1107_motherForbidden() throws Exception { postForbidden(); }
    @Test @WithMockUser(roles = "MODERATOR") void mccTc1108_moderatorForbidden() throws Exception { postForbidden(); }

    @Test void mccTc1109_noJwtUnauthorized() throws Exception {
        assertTrue(mappings.getHandlerMethods().keySet().stream().anyMatch(info ->
                info.getPatternValues().contains(URL) && info.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    private void postForbidden() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}
