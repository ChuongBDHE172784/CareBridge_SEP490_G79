package com.carebridge.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.partner.controller.PartnerContentRemovalController;
import com.carebridge.backend.partner.service.PartnerContentRemovalService;
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

@WebMvcTest(value = PartnerContentRemovalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RemovePartnerContentControllerSecurityTest {
    static final String URL = "/api/v1/admin/partner-content/SERVICE/f0200000-0000-0000-0000-00000000000d/remove";
    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean PartnerContentRemovalService service;
    @MockitoBean JwtTokenProvider jwt;
    @MockitoBean UserRepository users;

    @Test @WithMockUser(roles = "MODERATOR") void prcTc809_moderatorForbidden() throws Exception { request403(); }
    @Test @WithMockUser(roles = "PARTNER") void prcTc810_partnerForbidden() throws Exception { request403(); }
    @Test void prcTc811_noJwtUnauthorized() throws Exception {
        assertTrue(mappings.getHandlerMethods().keySet().stream().anyMatch(info ->
                info.getPatternValues().contains("/api/v1/admin/partner-content/{targetType}/{targetId}/remove")
                        && info.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"policy\"}"))
                .andExpect(status().isUnauthorized());
    }
    private void request403() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"policy\"}")).andExpect(status().isForbidden());
    }
}
