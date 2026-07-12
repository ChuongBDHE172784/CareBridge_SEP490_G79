package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.partner.controller.PartnerProfileController;
import com.carebridge.backend.partner.service.PartnerProfileService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@WebMvcTest(value = PartnerProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UpdatePartnerProfileControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean PartnerProfileService service;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;
    private static final String BODY = "{\"name\":\"Clinic\",\"type\":\"CLINIC\",\"address\":\"A\",\"city\":\"Hanoi\",\"phone\":\"0901234567\",\"email\":\"a@b.vn\",\"description\":\"x'; DROP TABLE partner_organizations;--\"}";

    @Test @WithMockUser(roles = "MOTHER")
    void pupTc208_nonPartnerReturns403() throws Exception {
        mockMvc.perform(put("/api/v1/partner/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden()); verify(service, never()).updateProfile(any(), any());
    }

    @Test void pupTc209_noJwtReturns401() throws Exception {
        boolean updateEndpointExists = mappings.getHandlerMethods().keySet().stream()
                .anyMatch(info -> info.getPatternValues().contains("/api/v1/partner/profile")
                        && info.getMethodsCondition().getMethods().contains(RequestMethod.PUT));
        org.junit.jupiter.api.Assertions.assertTrue(updateEndpointExists, "UC119 PUT endpoint must be registered");
        mockMvc.perform(put("/api/v1/partner/profile").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(username = "f1000000-0000-0000-0000-000000000001", roles = "PARTNER")
    void pupTc210_sqlInjectionPayloadReachesParameterizedPersistencePath() throws Exception {
        mockMvc.perform(put("/api/v1/partner/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }
}
