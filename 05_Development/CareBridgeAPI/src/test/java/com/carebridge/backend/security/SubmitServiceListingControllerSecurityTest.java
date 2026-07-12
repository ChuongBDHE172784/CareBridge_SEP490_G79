package com.carebridge.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(value=PartnerServiceController.class, excludeFilters=@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes=JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SubmitServiceListingControllerSecurityTest {
    static final String BODY="{\"serviceName\":\"Care\"}";
    @Autowired MockMvc mockMvc; @Autowired RequestMappingHandlerMapping mappings;
    @MockitoBean PartnerServiceService service; @MockitoBean JwtTokenProvider jwt; @MockitoBean UserRepository users;
    @Test @WithMockUser(roles="MOTHER") void pslTc309_nonPartnerReturns403() throws Exception { mockMvc.perform(post("/api/v1/partner/services").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden()); }
    @Test void pslTc310_noJwtReturns401() throws Exception {
        assertTrue(mappings.getHandlerMethods().keySet().stream().anyMatch(i->i.getPatternValues().contains("/api/v1/partner/services")&&i.getMethodsCondition().getMethods().contains(RequestMethod.POST)));
        mockMvc.perform(post("/api/v1/partner/services").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
    }
}
