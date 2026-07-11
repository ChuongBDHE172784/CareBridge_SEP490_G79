package com.carebridge.backend.partner;

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

@WebMvcTest(value = PartnerProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UpdatePartnerProfileControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PartnerProfileService service;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    @WithMockUser(username = "f1000000-0000-0000-0000-000000000001", roles = "PARTNER")
    void pupTc204_invalidEmailAndPhoneReturn400() throws Exception {
        mockMvc.perform(put("/api/v1/partner/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Clinic\",\"type\":\"CLINIC\",\"address\":\"A\",\"city\":\"Hanoi\",\"phone\":\"abc\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).updateProfile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
