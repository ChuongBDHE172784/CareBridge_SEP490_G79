package com.carebridge.backend.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.partner.controller.PartnerServiceController;
import com.carebridge.backend.partner.service.PartnerServiceService;
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

@WebMvcTest(value=PartnerServiceController.class, excludeFilters=@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes=JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SubmitServiceListingControllerTest {
    @Autowired MockMvc mockMvc; @MockitoBean PartnerServiceService service; @MockitoBean JwtTokenProvider jwt; @MockitoBean UserRepository users;
    @Test @WithMockUser(username="f3000000-0000-0000-0000-000000000001",roles="PARTNER")
    void pslTc304_invalidFieldsReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/partner/services").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceName\":\"\",\"priceFrom\":-1,\"bookingUrl\":\"bad\"}"))
                .andExpect(status().isBadRequest()); verify(service,never()).submitService(any(),any());
    }
}
